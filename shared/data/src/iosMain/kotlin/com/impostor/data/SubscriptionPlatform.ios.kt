package com.impostor.data

import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.PremiumProductId
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionProduct
import com.impostor.domain.SubscriptionState
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import kotlin.coroutines.resume

private const val REQUEST_NOTIFICATION = "com.impostor.storekit.request"
private const val RESPONSE_NOTIFICATION = "com.impostor.storekit.response"

actual fun platformStoreBillingClient(): StoreBillingClient = IosStoreKitBillingClient()

actual fun platformEntitlementStore(): EntitlementStore = UserDefaultsEntitlementStore()

actual fun platformTrialStore(): TrialStore = UserDefaultsTrialStore()

actual fun initializePlatformStoreBilling(context: Any) = Unit

private class IosStoreKitBillingClient : StoreBillingClient {
	override suspend fun loadProduct(): Result<SubscriptionProduct> = request("load") { response ->
		if (response["status"] == "success") {
			Result.success(
				SubscriptionProduct(
					productId = PremiumProductId,
					title = response["title"] as? String ?: "Impostor Premium",
					formattedPrice = response["price"] as? String ?: "",
					periodLabel = response["period"] as? String ?: "month",
				),
			)
		} else {
			Result.failure(response.error(SubscriptionError.PRODUCT_UNAVAILABLE))
		}
	}

	override suspend fun purchase(): Result<Entitlement> = request("purchase") { response ->
		response.toEntitlementResult(SubscriptionError.PURCHASE_FAILED)
	}

	override suspend fun restorePurchases(): Result<Entitlement> = request("restore") { response ->
		response.toEntitlementResult(SubscriptionError.RESTORE_FAILED)
	}

	override suspend fun openManageSubscriptions(): Result<Unit> = request("manage") { response ->
		if (response["status"] == "success") Result.success(Unit)
		else Result.failure(response.error(SubscriptionError.STORE_UNAVAILABLE))
	}

	private suspend fun <T> request(
		action: String,
		map: (Map<Any?, Any?>) -> Result<T>,
	): Result<T> = suspendCancellableCoroutine { continuation ->
		val center = NSNotificationCenter.defaultCenter
		val requestId = NSUUID.UUID().UUIDString
		var observer: Any? = null
		observer = center.addObserverForName(RESPONSE_NOTIFICATION, null, null) { notification ->
			val values: Map<Any?, *> = notification?.userInfo ?: return@addObserverForName
			if (values["requestId"] != requestId || !continuation.isActive) return@addObserverForName
			observer?.let(center::removeObserver)
			continuation.resume(map(values))
		}
		val values = mutableMapOf<Any?, Any?>(
			"requestId" to requestId,
			"action" to action,
			"productId" to PremiumProductId,
		)
		center.postNotificationName(REQUEST_NOTIFICATION, null, values)
		continuation.invokeOnCancellation { observer?.let(center::removeObserver) }
	}
}

private class UserDefaultsEntitlementStore : EntitlementStore {
	private val defaults = NSUserDefaults.standardUserDefaults

	override fun read(): CachedEntitlement? {
		val verifiedAt = defaults.doubleForKey(KEY_LAST_VERIFIED_AT)
		if (verifiedAt <= 0.0) return null
		val expiresAt = defaults.doubleForKey(KEY_EXPIRES_AT)
		return CachedEntitlement(
			entitled = defaults.boolForKey(KEY_ENTITLED),
			expiresAtMillis = expiresAt.toLong().takeIf { it > 0L },
			lastVerifiedAtMillis = verifiedAt.toLong(),
		)
	}

	override fun write(entitlement: CachedEntitlement) {
		defaults.setBool(entitlement.entitled, forKey = KEY_ENTITLED)
		defaults.setDouble((entitlement.expiresAtMillis ?: 0L).toDouble(), forKey = KEY_EXPIRES_AT)
		defaults.setDouble(entitlement.lastVerifiedAtMillis.toDouble(), forKey = KEY_LAST_VERIFIED_AT)
	}

	private companion object {
		const val KEY_ENTITLED = "entitled"
		const val KEY_EXPIRES_AT = "expires_at_millis"
		const val KEY_LAST_VERIFIED_AT = "last_verified_at_millis"
	}
}

private class UserDefaultsTrialStore : TrialStore {
	private val defaults = NSUserDefaults.standardUserDefaults

	override fun read(): CachedTrialState = CachedTrialState(
		startedGames = defaults.integerForKey(KEY_COMPLETED_GAMES).toInt(),
		lastStartedSessionId = defaults.stringForKey(KEY_LAST_SESSION_ID),
	)

	override fun write(state: CachedTrialState) {
		defaults.setInteger(state.startedGames.toLong(), forKey = KEY_COMPLETED_GAMES)
		if (state.lastStartedSessionId == null) {
			defaults.removeObjectForKey(KEY_LAST_SESSION_ID)
		} else {
			defaults.setObject(state.lastStartedSessionId, forKey = KEY_LAST_SESSION_ID)
		}
	}

	private companion object {
		// Keep legacy keys so an app update preserves the used trial allowance.
		const val KEY_COMPLETED_GAMES = "impostor_trial_completed_games"
		const val KEY_LAST_SESSION_ID = "impostor_trial_last_completed_session_id"
	}
}

private fun Map<Any?, Any?>.toEntitlementResult(fallback: SubscriptionError): Result<Entitlement> =
	when (this["status"]) {
		"active" -> Result.success(
			Entitlement(
				state = SubscriptionState.ACTIVE,
				expiresAtMillis = (this["expiresAtMillis"] as? Number)?.toLong(),
				source = EntitlementSource.STORE,
			),
		)
		"locked" -> Result.success(Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.STORE))
		else -> Result.failure(error(fallback))
	}

private fun Map<Any?, Any?>.error(default: SubscriptionError): SubscriptionException =
	SubscriptionException(
		when (this["error"]) {
			"cancelled" -> SubscriptionError.PURCHASE_CANCELLED
			"pending" -> SubscriptionError.PURCHASE_PENDING
			"unverified" -> SubscriptionError.PURCHASE_FAILED
			else -> default
		},
	)

