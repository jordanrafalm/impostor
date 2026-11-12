package com.impostor.data

import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.PremiumProductId
import com.impostor.domain.SubscriptionError
import com.impostor.domain.SubscriptionException
import com.impostor.domain.SubscriptionProduct
import com.impostor.domain.SubscriptionState
import com.impostor.domain.unlocksAllCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class SubscriptionRepositoryTest {
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
        val repository = SubscriptionRepositoryImpl(
            FakeBillingClient(purchaseResult = Result.success(activeEntitlement)),
            store,
            nowMillis = { 1_500 },
        )

        val result = repository.purchase()

        assertTrue(result.isSuccess)
        assertEquals(EntitlementSource.STORE, repository.entitlement.first().source)
        assertTrue(store.value!!.entitled)
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

        assertEquals(
            SubscriptionError.PURCHASE_PENDING,
            (pending.purchase().exceptionOrNull() as SubscriptionException).error,
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
        repository.purchase()

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
        val result = repository.purchase()

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
    ) : StoreBillingClient {
        override val entitlementUpdates: Flow<Entitlement>? get() = updates

        override suspend fun loadProduct() = Result.success(SubscriptionProduct(PremiumProductId, "Premium", "$1", "month"))
        override suspend fun queryCurrentEntitlement() = queryResult
        override suspend fun purchase() = purchaseResult
        override suspend fun restorePurchases() = restoreResult
        override suspend fun openManageSubscriptions() = Result.success(Unit)
    }

    private companion object {
        val activeEntitlement = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        val lockedEntitlement = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.STORE)
    }
}