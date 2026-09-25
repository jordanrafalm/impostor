package com.impostor.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.Intent
import android.net.Uri
import java.lang.ref.WeakReference
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.PremiumProductId
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionProduct
import com.impostor.domain.SubscriptionState
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume

private var billingContext: Context? = null
private var billingActivity: WeakReference<Activity>? = null

private const val TRIAL_PREFERENCES = "game_trial"

actual fun initializePlatformStoreBilling(context: Any) {
	val activity = context as? Activity ?: return
	billingActivity = WeakReference(activity)
	billingContext = activity.applicationContext
}

actual fun platformStoreBillingClient(): StoreBillingClient =
	billingContext?.let { AndroidStoreBillingClient(it) } ?: unavailableStoreBillingClient()

actual fun platformEntitlementStore(): EntitlementStore = billingContext?.let {
	SharedPreferencesEntitlementStore(it.getSharedPreferences("subscription_entitlement", Context.MODE_PRIVATE))
} ?: fallbackEntitlementStore()

actual fun platformTrialStore(): TrialStore = billingContext?.let {
	SharedPreferencesTrialStore(it.getSharedPreferences(TRIAL_PREFERENCES, Context.MODE_PRIVATE))
} ?: fallbackTrialStore()

private class AndroidStoreBillingClient(
	private val context: Context,
) : StoreBillingClient {
	private var productDetails: ProductDetails? = null

	private suspend fun connectedClient(): Result<BillingClient> = suspendCancellableCoroutine { continuation ->
		val client = BillingClient.newBuilder(context)
			.setListener { _, _ -> }
			.enablePendingPurchases(
				PendingPurchasesParams.newBuilder()
					.enableOneTimeProducts()
					.build(),
			)
			.build()
		client.startConnection(object : BillingClientStateListener {
			override fun onBillingSetupFinished(result: BillingResult) {
				if (result.responseCode == BillingResponseCode.OK) {
					continuation.resume(Result.success(client))
				} else {
					client.endConnection()
					continuation.resume(Result.failure(result.toSubscriptionException(SubscriptionError.STORE_UNAVAILABLE)))
				}
			}

			override fun onBillingServiceDisconnected() {
				if (continuation.isActive) {
					client.endConnection()
					continuation.resume(Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE)))
				}
			}
		})
		continuation.invokeOnCancellation { client.endConnection() }
	}

	override suspend fun loadProduct(): Result<SubscriptionProduct> {
		val client = connectedClient().getOrElse { return Result.failure(it) }
		return suspendCancellableCoroutine { continuation ->
			client.queryProductDetailsAsync(
				QueryProductDetailsParams.newBuilder()
					.setProductList(
						listOf(
							QueryProductDetailsParams.Product.newBuilder()
								.setProductId(PremiumProductId)
								.setProductType(ProductType.SUBS)
								.build(),
						),
					)
					.build(),
			) { result, details ->
				client.endConnection()
				val detail = details.productDetailsList.firstOrNull()
				if (result.responseCode != BillingResponseCode.OK || detail == null) {
					continuation.resume(Result.failure(result.toSubscriptionException(SubscriptionError.PRODUCT_UNAVAILABLE)))
				} else {
					productDetails = detail
					val offer = detail.subscriptionOfferDetails?.firstOrNull()
					val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
					continuation.resume(
						Result.success(
							SubscriptionProduct(
								productId = detail.productId,
								title = detail.title,
								formattedPrice = phase?.formattedPrice ?: detail.oneTimePurchaseOfferDetails?.formattedPrice.orEmpty(),
								periodLabel = phase?.billingPeriod?.toPeriodLabel() ?: "month",
							),
						),
					)
				}
			}
		}
	}

	override suspend fun purchase(): Result<Entitlement> {
		val activity = billingActivity?.get()
			?: return Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE))
		val details = productDetails ?: return Result.failure(SubscriptionException(SubscriptionError.PRODUCT_UNAVAILABLE))
		val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
			?: return Result.failure(SubscriptionException(SubscriptionError.PRODUCT_UNAVAILABLE))
		return suspendCancellableCoroutine { continuation ->
			lateinit var listenerClient: BillingClient
			listenerClient = BillingClient.newBuilder(context)
				.setListener { result, purchases ->
					if (result.responseCode != BillingResponseCode.OK) {
						listenerClient.endConnection()
						continuation.resume(Result.failure(result.toSubscriptionException(result.toSubscriptionError())))
					} else {
						handlePurchases(purchases.orEmpty(), continuation, listenerClient)
					}
				}
				.enablePendingPurchases(
					PendingPurchasesParams.newBuilder()
						.enableOneTimeProducts()
						.build(),
				)
				.build()
			listenerClient.startConnection(object : BillingClientStateListener {
				override fun onBillingSetupFinished(result: BillingResult) {
					if (result.responseCode != BillingResponseCode.OK) {
						listenerClient.endConnection()
						continuation.resume(Result.failure(result.toSubscriptionException(SubscriptionError.STORE_UNAVAILABLE)))
						return
					}
					val flowResult = listenerClient.launchBillingFlow(
						activity,
						BillingFlowParams.newBuilder()
							.setProductDetailsParamsList(
								listOf(
									BillingFlowParams.ProductDetailsParams.newBuilder()
										.setProductDetails(details)
										.setOfferToken(offerToken)
										.build(),
								),
							)
							.build(),
					)
					if (flowResult.responseCode != BillingResponseCode.OK) {
						listenerClient.endConnection()
						continuation.resume(Result.failure(flowResult.toSubscriptionException(flowResult.toSubscriptionError())))
					}
				}

				override fun onBillingServiceDisconnected() {
					if (continuation.isActive) {
						listenerClient.endConnection()
						continuation.resume(Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE)))
					}
				}
			})
			continuation.invokeOnCancellation { listenerClient.endConnection() }
		}
	}

	override suspend fun restorePurchases(): Result<Entitlement> {
		val client = connectedClient().getOrElse { return Result.failure(it) }
		return suspendCancellableCoroutine { continuation ->
			client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()) { result, purchases ->
				if (result.responseCode != BillingResponseCode.OK) {
					client.endConnection()
					continuation.resume(Result.failure(result.toSubscriptionException(SubscriptionError.RESTORE_FAILED)))
				} else {
					handlePurchases(purchases, continuation, client)
				}
			}
			continuation.invokeOnCancellation { client.endConnection() }
		}
	}

	override suspend fun openManageSubscriptions(): Result<Unit> {
		val intent = Intent(
			Intent.ACTION_VIEW,
			Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}&sku=$PremiumProductId"),
		).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		return runCatching { context.startActivity(intent) }
	}

	private fun handlePurchases(
		purchases: List<Purchase>,
		continuation: CancellableContinuation<Result<Entitlement>>,
		client: BillingClient? = null,
	) {
		val purchase = purchases.firstOrNull { PremiumProductId in it.products }
		when {
			purchase == null -> finish(Result.success(lockedEntitlement()), continuation, client)
			purchase.purchaseState == Purchase.PurchaseState.PENDING ->
				finish(Result.failure(SubscriptionException(SubscriptionError.PURCHASE_PENDING)), continuation, client)
			purchase.purchaseState != Purchase.PurchaseState.PURCHASED ->
				finish(Result.failure(SubscriptionException(SubscriptionError.PURCHASE_FAILED)), continuation, client)
			purchase.isAcknowledged -> finish(Result.success(activeEntitlement()), continuation, client)
			else -> client?.acknowledgePurchase(
				com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder()
					.setPurchaseToken(purchase.purchaseToken)
					.build(),
			) { result ->
				if (result.responseCode == BillingResponseCode.OK) {
					finish(Result.success(activeEntitlement()), continuation, client)
				} else {
					finish(Result.failure(result.toSubscriptionException(SubscriptionError.PURCHASE_FAILED)), continuation, client)
				}
			} ?: finish(Result.failure(SubscriptionException(SubscriptionError.PURCHASE_FAILED)), continuation, client)
		}
	}

	private fun finish(
		result: Result<Entitlement>,
		continuation: CancellableContinuation<Result<Entitlement>>,
		client: BillingClient?,
	) {
		client?.endConnection()
		if (continuation.isActive) continuation.resume(result)
	}
}

private class SharedPreferencesEntitlementStore(
	private val preferences: SharedPreferences,
) : EntitlementStore {
	override fun read(): CachedEntitlement? {
		val verifiedAt = preferences.getLong(KEY_LAST_VERIFIED_AT, 0L)
		if (verifiedAt <= 0L) return null
		val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
		return CachedEntitlement(
			entitled = preferences.getBoolean(KEY_ENTITLED, false),
			expiresAtMillis = expiresAt.takeIf { it > 0L },
			lastVerifiedAtMillis = verifiedAt,
		)
	}

	override fun write(entitlement: CachedEntitlement) {
		preferences.edit()
			.putBoolean(KEY_ENTITLED, entitlement.entitled)
			.putLong(KEY_EXPIRES_AT, entitlement.expiresAtMillis ?: 0L)
			.putLong(KEY_LAST_VERIFIED_AT, entitlement.lastVerifiedAtMillis)
			.apply()
	}

	private companion object {
		const val KEY_ENTITLED = "entitled"
		const val KEY_EXPIRES_AT = "expires_at_millis"
		const val KEY_LAST_VERIFIED_AT = "last_verified_at_millis"
	}
}

private class SharedPreferencesTrialStore(
	private val preferences: SharedPreferences,
) : TrialStore {
	override fun read(): CachedTrialState = CachedTrialState(
		startedGames = preferences.getInt(KEY_COMPLETED_GAMES, 0),
		lastStartedSessionId = preferences.getString(KEY_LAST_SESSION_ID, null),
	)

	override fun write(state: CachedTrialState) {
		preferences.edit()
			.putInt(KEY_COMPLETED_GAMES, state.startedGames)
			.putString(KEY_LAST_SESSION_ID, state.lastStartedSessionId)
			.apply()
	}

	private companion object {
		// Keep legacy keys so an app update preserves the used trial allowance.
		const val KEY_COMPLETED_GAMES = "completed_games"
		const val KEY_LAST_SESSION_ID = "last_completed_session_id"
	}
}

private fun activeEntitlement() = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)

private fun lockedEntitlement() = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.STORE)

private fun String.toPeriodLabel(): String = when (this) {
	"P1W" -> "week"
	"P1M" -> "month"
	"P3M" -> "3 months"
	"P6M" -> "6 months"
	"P1Y" -> "year"
	else -> this
}

private fun BillingResult.toSubscriptionError(): SubscriptionError = when (responseCode) {
	BillingResponseCode.USER_CANCELED -> SubscriptionError.PURCHASE_CANCELLED
	BillingResponseCode.ITEM_ALREADY_OWNED -> SubscriptionError.ALREADY_OWNED
	BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.BILLING_UNAVAILABLE -> SubscriptionError.STORE_UNAVAILABLE
	else -> SubscriptionError.PURCHASE_FAILED
}

private fun BillingResult.toSubscriptionException(default: SubscriptionError) =
	SubscriptionException(if (responseCode == BillingResponseCode.OK) default else toSubscriptionError())