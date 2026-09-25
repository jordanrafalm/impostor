package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SubscriptionTest {
    @Test
    fun `should allow exactly three trial starts without a subscription`() {
        val locked = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.LOCAL_CACHE)
        for (used in 0..2) assertTrue(TrialState(used).canStartGame(locked))
        assertFalse(TrialState(3).canStartGame(locked))
    }

    @Test
    fun `should allow verified subscribers after trial exhaustion`() {
        val active = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        assertTrue(TrialState(3).canStartGame(active))
        assertFalse(TrialState(3).canStartGame(active.copy(source = EntitlementSource.LOCAL_CACHE)))
        assertFalse(TrialState(3).canStartGame(active.copy(state = SubscriptionState.EXPIRED)))
    }

    @Test
    fun `should keep all categories free until the third started game`() {
        assertTrue(TrialState(0).hasFreeCategories)
        assertTrue(TrialState(2).hasFreeCategories)
        assertEquals(1, TrialState(2).remainingFreeGames)
        assertFalse(TrialState(3).hasFreeCategories)
        assertEquals(0, TrialState(3).remainingFreeGames)
    }

    @Test
    fun `should unlock all categories only for active store entitlement`() {
        assertTrue(Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE).unlocksAllCategories())
        assertFalse(Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.LOCAL_CACHE).unlocksAllCategories())
    }

    @Test
    fun `should not unlock expired revoked pending or unavailable entitlement`() {
        listOf(
            SubscriptionState.EXPIRED,
            SubscriptionState.LOCKED,
            SubscriptionState.PURCHASING,
            SubscriptionState.REVOKED,
            SubscriptionState.RESTORING,
            SubscriptionState.UNAVAILABLE,
        ).forEach { state ->
            assertFalse(Entitlement(state = state, source = EntitlementSource.STORE).unlocksAllCategories())
        }
    }
}