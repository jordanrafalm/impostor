package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeScreenVisibilityTest {
    @Test
    fun `premium unlock appears when free games are exhausted`() {
        assertTrue(
            shouldShowPremiumUnlock(
                remainingFreeGames = 0,
                premiumUnlocked = false,
                revealedByHold = false,
            ),
        )
    }

    @Test
    fun `premium unlock appears after holding play even with free games remaining`() {
        assertTrue(
            shouldShowPremiumUnlock(
                remainingFreeGames = 2,
                premiumUnlocked = false,
                revealedByHold = true,
            ),
        )
    }

    @Test
    fun `premium unlock stays hidden while free games remain and play was not held`() {
        assertFalse(
            shouldShowPremiumUnlock(
                remainingFreeGames = 1,
                premiumUnlocked = false,
                revealedByHold = false,
            ),
        )
    }

    @Test
    fun `premium unlock stays hidden for premium users`() {
        assertFalse(
            shouldShowPremiumUnlock(
                remainingFreeGames = 0,
                premiumUnlocked = true,
                revealedByHold = true,
            ),
        )
    }
}
