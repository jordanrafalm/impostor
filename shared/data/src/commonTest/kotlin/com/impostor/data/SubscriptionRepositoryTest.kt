package com.impostor.data

import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.MonthlyPriceNoticeEligibility
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionProduct
import com.impostor.domain.SubscriptionPlan
import com.impostor.domain.SubscriptionState
import com.impostor.domain.unlocksAllCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionRepositoryTest {
    @Test
    fun `notice result is independent of cached entitlement and does not change it`() = runTest {
        val eligibility = MonthlyPriceNoticeEligibility.Eligible("old", "new")
        val store = FakeEntitlementStore(CachedEntitlement(true, null, 1_000))
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(noticeResult = eligibility), store, nowMillis = { 1_500 },
        )
        assertEquals(eligibility, repository.monthlyPriceNoticeEligibility())
        assertFalse(repository.entitlement.first().unlocksAllCategories())
        assertEquals(CachedEntitlement(true, null, 1_000), store.value)
    }

    @Test
    fun `unsupported and unknown notice outcomes stay distinct`() = runTest {
        val unsupported = SubscriptionRepositoryImpl(unavailableStoreBillingClient(), FakeEntitlementStore())
        val unknown = SubscriptionRepositoryImpl(
            FakeBillingClient(noticeResult = MonthlyPriceNoticeEligibility.Unknown), FakeEntitlementStore(),
        )
        assertEquals(MonthlyPriceNoticeEligibility.Ineligible, unsupported.monthlyPriceNoticeEligibility())
        assertEquals(MonthlyPriceNoticeEligibility.Unknown, unknown.monthlyPriceNoticeEligibility())
    }

    @Test
    fun `blank formatted prices are unknown`() = runTest {
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(noticeResult = MonthlyPriceNoticeEligibility.Eligible("", "new")),
            FakeEntitlementStore(),
        )
        assertEquals(MonthlyPriceNoticeEligibility.Unknown, repository.monthlyPriceNoticeEligibility())
    }

    @Test
    fun `notice errors are unknown without altering entitlement`() = runTest {
        val store = FakeEntitlementStore()
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(noticeError = IllegalStateException()), store,
        )
        assertEquals(MonthlyPriceNoticeEligibility.Unknown, repository.monthlyPriceNoticeEligibility())
        assertEquals(SubscriptionState.LOCKED, repository.entitlement.first().state)
        assertEquals(null, store.value)
    }

    @Test
    fun `notice cancellation propagates`() = runTest {
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(noticeError = CancellationException("cancelled")), FakeEntitlementStore(),
        )
        var cancelled = false
        try {
            repository.monthlyPriceNoticeEligibility()
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }

    @Test
    fun `should expose cached active entitlement without granting store source`() = runTest {
        val store = FakeEntitlementStore(CachedEntitlement(true, 2_000, 1_000))
        val repository = SubscriptionRepositoryImpl(FakeBillingClient(), store, nowMillis = { 1_500 })

        val entitlement = repository.entitlement.first()

        assertEquals(SubscriptionState.ACTIVE, entitlement.state)
        assertEquals(EntitlementSource.LOCAL_CACHE, entitlement.source)
        assertFalse(entitlement.unlocksAllCategories())
    }

    @Test
    fun `should reconcile cached entitlement against current store state`() = runTest {
        val store = FakeEntitlementStore(CachedEntitlement(true, 2_000, 1_000))
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(queryResult = Result.success(activeEntitlement)),
            store,
            nowMillis = { 1_500 },
            updateScope = backgroundScope,
        )

        val result = repository.reconcileEntitlement()

        assertTrue(result.isSuccess)
        assertTrue(repository.entitlement.first().unlocksAllCategories())
        assertEquals(EntitlementSource.STORE, repository.entitlement.first().source)
    }

    @Test
    fun `should replace cached premium with locked verified store state`() = runTest {
        val store = FakeEntitlementStore(CachedEntitlement(true, null, 1_000))
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(queryResult = Result.success(lockedEntitlement)),
            store,
            updateScope = backgroundScope,
        )

        assertTrue(repository.reconcileEntitlement().isSuccess)

        assertEquals(SubscriptionState.LOCKED, repository.entitlement.first().state)
        assertFalse(repository.entitlement.first().unlocksAllCategories())
        assertFalse(store.value!!.entitled)
    }

    @Test
    fun `should not grant cached entitlement when store reconciliation fails`() = runTest {
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(
                queryResult = Result.failure(SubscriptionException(SubscriptionError.STORE_UNAVAILABLE)),
            ),
            FakeEntitlementStore(CachedEntitlement(true, null, 1_000)),
            updateScope = backgroundScope,
        )

        val result = repository.reconcileEntitlement()
        val entitlement = repository.entitlement.first()

        assertTrue(result.isFailure)
        assertEquals(SubscriptionState.UNAVAILABLE, entitlement.state)
        assertFalse(entitlement.unlocksAllCategories())
    }

    @Test
    fun `should propagate store transaction updates to shared entitlement`() = runTest {
        val updates = MutableSharedFlow<Entitlement>(extraBufferCapacity = 1)
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(updates = updates),
            FakeEntitlementStore(),
            updateScope = backgroundScope,
        )
        runCurrent()

        updates.emit(Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.LOCAL_CACHE))
        runCurrent()
        assertTrue(repository.entitlement.first().unlocksAllCategories())

        updates.emit(Entitlement(state = SubscriptionState.REVOKED, source = EntitlementSource.STORE))
        runCurrent()

        assertEquals(SubscriptionState.REVOKED, repository.entitlement.first().state)
        assertFalse(repository.entitlement.first().unlocksAllCategories())
    }

    @Test
    fun `should replace cache with active verified store entitlement`() = runTest {
        val store = FakeEntitlementStore()
        val billing = FakeBillingClient(purchaseResult = Result.success(activeEntitlement))
        val repository = SubscriptionRepositoryImpl(
            billing,
            store,
            nowMillis = { 1_500 },
        )

        repository.loadProducts()
        val result = repository.purchase(SubscriptionPlan.MONTHLY)

        assertTrue(result.isSuccess)
        assertEquals(SubscriptionPlan.MONTHLY, billing.purchasedPlan)
        assertEquals(EntitlementSource.STORE, repository.entitlement.first().source)
        assertTrue(store.value!!.entitled)
    }

    @Test
    fun `should not purchase a plan that was not loaded from the store`() = runTest {
        val billing = FakeBillingClient(availablePlans = listOf(SubscriptionPlan.MONTHLY))
        val repository = SubscriptionRepositoryImpl(billing, FakeEntitlementStore())
        repository.loadProducts()

        val result = repository.purchase(SubscriptionPlan.YEARLY)

        assertEquals(SubscriptionError.PRODUCT_UNAVAILABLE, (result.exceptionOrNull() as SubscriptionException).error)
        assertEquals(null, billing.purchasedPlan)
    }

    @Test
    fun `should pass the selected yearly plan through to billing`() = runTest {
        val billing = FakeBillingClient()
        val repository = SubscriptionRepositoryImpl(billing, FakeEntitlementStore())
        repository.loadProducts()

        assertTrue(repository.purchase(SubscriptionPlan.YEARLY).isSuccess)
        assertEquals(SubscriptionPlan.YEARLY, billing.purchasedPlan)
    }

    @Test
    fun `should not unlock for pending or restore without entitlement`() = runTest {
        val pending = SubscriptionRepositoryImpl(
            FakeBillingClient(purchaseResult = Result.failure(SubscriptionException(SubscriptionError.PURCHASE_PENDING))),
            FakeEntitlementStore(),
        )
        val none = SubscriptionRepositoryImpl(
            FakeBillingClient(restoreResult = Result.success(lockedEntitlement)),
            FakeEntitlementStore(),
        )

        pending.loadProducts()
        assertEquals(
            SubscriptionError.PURCHASE_PENDING,
            (pending.purchase(SubscriptionPlan.MONTHLY).exceptionOrNull() as SubscriptionException).error,
        )
        assertFalse(none.restorePurchases().getOrThrow().state == SubscriptionState.ACTIVE)
    }

    @Test
    fun `should preserve active entitlement when a later purchase fails`() = runTest {
        // Given
        val store = FakeEntitlementStore(CachedEntitlement(true, null, 1_000))
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(purchaseResult = Result.failure(SubscriptionException(SubscriptionError.PURCHASE_CANCELLED))),
            store,
            nowMillis = { 1_500 },
        )

        // When
        repository.loadProducts()
        repository.purchase(SubscriptionPlan.MONTHLY)

        // Then
        val entitlement = repository.entitlement.first()
        assertEquals(SubscriptionState.ACTIVE, entitlement.state)
        assertEquals(EntitlementSource.LOCAL_CACHE, entitlement.source)
    }

    @Test
    fun `should leave purchasing state and mark store unavailable after network failure`() = runTest {
        // Given
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(
                purchaseResult = Result.failure(SubscriptionException(SubscriptionError.NETWORK_UNAVAILABLE)),
            ),
            FakeEntitlementStore(),
        )

        // When
        repository.loadProducts()
        val result = repository.purchase(SubscriptionPlan.MONTHLY)

        // Then
        assertEquals(SubscriptionError.NETWORK_UNAVAILABLE, (result.exceptionOrNull() as SubscriptionException).error)
        assertEquals(SubscriptionState.UNAVAILABLE, repository.entitlement.first().state)
    }

    private class FakeEntitlementStore(initial: CachedEntitlement? = null) : EntitlementStore {
        var value: CachedEntitlement? = initial
        override fun read(): CachedEntitlement? = value
        override fun write(entitlement: CachedEntitlement) { value = entitlement }
    }

    private class FakeBillingClient(
        private val purchaseResult: Result<Entitlement> = Result.success(activeEntitlement),
        private val restoreResult: Result<Entitlement> = Result.success(lockedEntitlement),
        private val queryResult: Result<Entitlement> = Result.success(lockedEntitlement),
        private val updates: Flow<Entitlement>? = null,
        private val availablePlans: List<SubscriptionPlan> = SubscriptionPlan.entries,
        private val noticeResult: MonthlyPriceNoticeEligibility = MonthlyPriceNoticeEligibility.Ineligible,
        private val noticeError: Exception? = null,
    ) : StoreBillingClient {
        override val entitlementUpdates: Flow<Entitlement>? get() = updates
        var purchasedPlan: SubscriptionPlan? = null

        override suspend fun monthlyPriceNoticeEligibility(): MonthlyPriceNoticeEligibility {
            noticeError?.let { throw it }
            return noticeResult
        }
        override suspend fun loadProducts() = Result.success(
            availablePlans.map { plan ->
                SubscriptionProduct(plan, plan.productId, "Premium", "$1", "month")
            },
        )
        override suspend fun queryCurrentEntitlement() = queryResult
        override suspend fun purchase(plan: SubscriptionPlan): Result<Entitlement> {
            purchasedPlan = plan
            return purchaseResult
        }
        override suspend fun restorePurchases() = restoreResult
        override suspend fun openManageSubscriptions() = Result.success(Unit)
    }

    private companion object {
        val activeEntitlement = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        val lockedEntitlement = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.STORE)
    }
}