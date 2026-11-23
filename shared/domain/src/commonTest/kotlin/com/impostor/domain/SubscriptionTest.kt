package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SubscriptionTest {
    @Test
    fun `should include promotion only after it has been claimed`() {
        assertEquals(0, TrialState(5, blackFridayGamesUsed = 0).remainingFreeGames)
        assertEquals(5, TrialState(5, blackFridayClaimed = true).remainingFreeGames)
        assertEquals(3, TrialState(5, blackFridayClaimed = true, blackFridayGamesUsed = 2).remainingFreeGames)
        assertFalse(TrialState(5, blackFridayClaimed = true, blackFridayGamesUsed = 5).hasFreeCategories)
    }

    @Test
    fun `should charge only non premium starts containing a limited category`() {
        val locked = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.LOCAL_CACHE)
        val premium = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        assertFalse(requiresTrialGame(setOf("animals", "places"), locked))
        assertTrue(requiresTrialGame(setOf("animals", "sex"), locked))
        assertFalse(requiresTrialGame(setOf("sex"), premium))
        assertFalse(requiresTrialGame(emptySet(), locked))
    }

    @Test
    fun `should allow exactly three trial starts without a subscription`() {
        val locked = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.LOCAL_CACHE)
        for (used in 0..2) assertTrue(TrialState(used).canStartGame(locked))
        assertFalse(TrialState(5).canStartGame(locked))
    }

    @Test
    fun `should allow verified subscribers after trial exhaustion`() {
        val active = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        assertTrue(TrialState(5).canStartGame(active))
        assertFalse(TrialState(5).canStartGame(active.copy(source = EntitlementSource.LOCAL_CACHE)))
        assertFalse(TrialState(5).canStartGame(active.copy(state = SubscriptionState.EXPIRED)))
    }

    @Test
    fun `should keep all categories free until the fifth started game`() {
        assertTrue(TrialState(0).hasFreeCategories)
        assertTrue(TrialState(4).hasFreeCategories)
        assertEquals(1, TrialState(4).remainingFreeGames)
        assertFalse(TrialState(5).hasFreeCategories)
        assertEquals(0, TrialState(5).remainingFreeGames)
    }

    @Test
    fun `should unlock all categories only for active store entitlement`() {
        assertTrue(Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE).unlocksAllCategories())
        assertFalse(Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.LOCAL_CACHE).unlocksAllCategories())
    }

    @Test
    fun `should map only the configured monthly and yearly product ids`() {
        assertEquals(SubscriptionPlan.MONTHLY, SubscriptionPlan.fromProductId("com.impostor.app.premium.monthly"))
        assertEquals(SubscriptionPlan.YEARLY, SubscriptionPlan.fromProductId("com.impostor.app.premium.yearly"))
        assertEquals(null, SubscriptionPlan.fromProductId("com.impostor.app.premium.unknown"))
        assertEquals("com.impostor.app.premium.monthly", SubscriptionPlan.MONTHLY.productId)
        assertEquals("com.impostor.app.premium.yearly", SubscriptionPlan.YEARLY.productId)
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