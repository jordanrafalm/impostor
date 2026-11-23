package com.impostor.data

import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.MonthlyPriceNoticeEligibility
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionProduct
import com.impostor.domain.SubscriptionPlan
import com.impostor.domain.SubscriptionRepository
import com.impostor.domain.SubscriptionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

interface StoreBillingClient {
    val entitlementUpdates: Flow<Entitlement>?
        get() = null

    suspend fun loadProducts(): Result<List<SubscriptionProduct>>
    suspend fun monthlyPriceNoticeEligibility(): MonthlyPriceNoticeEligibility
    suspend fun queryCurrentEntitlement(): Result<Entitlement> = restorePurchases()
    suspend fun purchase(plan: SubscriptionPlan): Result<Entitlement>
    suspend fun restorePurchases(): Result<Entitlement>
    suspend fun openManageSubscriptions(): Result<Unit>
}

interface EntitlementStore {
    fun read(): CachedEntitlement?
    fun write(entitlement: CachedEntitlement)
}

data class CachedEntitlement(
    val entitled: Boolean,
    val expiresAtMillis: Long?,
    val lastVerifiedAtMillis: Long,
)

private class InMemoryEntitlementStore : EntitlementStore {
    private var value: CachedEntitlement? = null

    override fun read(): CachedEntitlement? = value

    override fun write(entitlement: CachedEntitlement) {
        value = entitlement
    }
}

private val fallbackStore = InMemoryEntitlementStore()

expect fun platformStoreBillingClient(): StoreBillingClient
expect fun platformEntitlementStore(): EntitlementStore

expect fun initializePlatformStoreBilling(context: Any)

@OptIn(kotlin.time.ExperimentalTime::class)
class SubscriptionRepositoryImpl(
    private val billingClient: StoreBillingClient = platformStoreBillingClient(),
    private val entitlementStore: EntitlementStore = platformEntitlementStore(),
    private val nowMillis: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    updateScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SubscriptionRepository {
    private val transitionMutex = Mutex()
    private val productMutex = Mutex()
    private var availablePlans: Set<SubscriptionPlan> = emptySet()
    private val initial = entitlementStore.read()?.toEntitlement(nowMillis()) ?:
        Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.LOCAL_CACHE)
    private val _entitlement = MutableStateFlow(initial)

    override val entitlement: Flow<Entitlement> = _entitlement.asStateFlow()

    override suspend fun monthlyPriceNoticeEligibility(): MonthlyPriceNoticeEligibility = try {
        when (val result = billingClient.monthlyPriceNoticeEligibility()) {
            is MonthlyPriceNoticeEligibility.Eligible ->
                if (result.renewalPrice.isBlank() || result.currentPrice.isBlank()) {
                    MonthlyPriceNoticeEligibility.Unknown
                } else {
                    result
                }
            else -> result
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        MonthlyPriceNoticeEligibility.Unknown
    }

    init {
        billingClient.entitlementUpdates?.let { updates ->
            updateScope.launch {
                updates.collect { update ->
                    transitionMutex.withLock { storeVerified(update) }
                }
            }
        }
    }

    override suspend fun loadProducts(): Result<List<SubscriptionProduct>> {
        val result = billingClient.loadProducts()
        result.onSuccess { products ->
            val verifiedProducts = products.filter {
                it.productId == it.plan.productId && it.formattedPrice.isNotBlank()
            }
            productMutex.withLock { availablePlans = verifiedProducts.mapTo(mutableSetOf()) { it.plan } }
            return Result.success(verifiedProducts)
        }
        productMutex.withLock { availablePlans = emptySet() }
        return result
    }

    override suspend fun reconcileEntitlement(): Result<Entitlement> = transitionMutex.withLock {
        val result = try {
            billingClient.queryCurrentEntitlement()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
        result.onSuccess(::storeVerified).onFailure {
            _entitlement.value = _entitlement.value.copy(state = SubscriptionState.UNAVAILABLE)
        }
        result
    }

    override suspend fun purchase(plan: SubscriptionPlan): Result<Entitlement> {
        val available = productMutex.withLock { plan in availablePlans }
        if (!available) {
            return Result.failure(SubscriptionException(SubscriptionError.PRODUCT_UNAVAILABLE))
        }
        return transition(SubscriptionState.PURCHASING) { billingClient.purchase(plan) }
    }

    override suspend fun restorePurchases(): Result<Entitlement> = transition(SubscriptionState.RESTORING) {
        billingClient.restorePurchases()
    }

    override suspend fun openManageSubscriptions(): Result<Unit> = billingClient.openManageSubscriptions()

    private suspend fun transition(
        transientState: SubscriptionState,
        action: suspend () -> Result<Entitlement>,
    ): Result<Entitlement> = transitionMutex.withLock {
        val previous = _entitlement.value
        _entitlement.value = Entitlement(
            state = transientState,
            expiresAtMillis = previous.expiresAtMillis,
            source = previous.source,
        )
        val result = try {
            action()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
        result.onSuccess { storeVerified(it) }
            .onFailure { _entitlement.value = errorEntitlement(it, previous) }
        result
    }

    private fun storeVerified(entitlement: Entitlement) {
        val verified = entitlement.copy(source = EntitlementSource.STORE)
        _entitlement.value = verified
        entitlementStore.write(
            CachedEntitlement(
                entitled = verified.state == SubscriptionState.ACTIVE,
                expiresAtMillis = verified.expiresAtMillis,
                lastVerifiedAtMillis = nowMillis(),
            ),
        )
    }

    private fun errorEntitlement(error: Throwable, previous: Entitlement): Entitlement = previous.copy(
        state = when ((error as? SubscriptionException)?.error) {
            SubscriptionError.PURCHASE_PENDING -> SubscriptionState.PURCHASING
            SubscriptionError.NETWORK_UNAVAILABLE,
            SubscriptionError.STORE_UNAVAILABLE,
            SubscriptionError.PRODUCT_UNAVAILABLE,
            -> SubscriptionState.UNAVAILABLE
            else -> previous.state
        },
    )
}

private fun CachedEntitlement.toEntitlement(nowMillis: Long): Entitlement = Entitlement(
    state = if (entitled && (expiresAtMillis == null || expiresAtMillis > nowMillis)) {
        SubscriptionState.ACTIVE
    } else if (entitled) {
        SubscriptionState.EXPIRED
    } else {
        SubscriptionState.LOCKED
    },
    expiresAtMillis = expiresAtMillis,
    source = EntitlementSource.LOCAL_CACHE,
)

internal fun fallbackEntitlementStore(): EntitlementStore = fallbackStore

internal fun unavailableStoreBillingClient(): StoreBillingClient = object : StoreBillingClient {
    override suspend fun monthlyPriceNoticeEligibility(): MonthlyPriceNoticeEligibility =
        MonthlyPriceNoticeEligibility.Ineligible
    override suspend fun loadProducts(): Result<List<SubscriptionProduct>> =
        Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE))

    override suspend fun purchase(plan: SubscriptionPlan): Result<Entitlement> =
        Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE))

    override suspend fun restorePurchases(): Result<Entitlement> =
        Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE))

    override suspend fun openManageSubscriptions(): Result<Unit> =
        Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE))
}