package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ReviewPromptTest {
    private val store = FakePreferenceStore()
    private val sut = ReviewPromptPreferences(store)
    private val week = 7L * 24 * 60 * 60 * 1000

    @Test
    fun `should ask only after one completed game and retain count across instances`() {
        assertFalse(sut.shouldShowAtLaunch(100, false))
        sut.recordCompletedGame()
        assertTrue(ReviewPromptPreferences(store).shouldShowAtLaunch(100, false))
    }

    @Test
    fun `should require both a week and one more game after postponing`() {
        sut.recordCompletedGame()
        sut.postpone(100)

        assertFalse(sut.shouldShowAtLaunch(100 + week, false))
        sut.recordCompletedGame()
        assertFalse(sut.shouldShowAtLaunch(99 + week, false))
        assertTrue(ReviewPromptPreferences(store).shouldShowAtLaunch(100 + week, false))
    }

    @Test
    fun `should never ask again after opening the store`() {
        sut.markStoreOpened()

        repeat(10) { sut.recordCompletedGame() }
        assertFalse(ReviewPromptPreferences(store).shouldShowAtLaunch(week * 10, false))
    }

    @Test
    fun `should not bypass cooldown when the clock moves backwards`() {
        sut.postpone(week)

        repeat(4) { sut.recordCompletedGame() }
        assertFalse(sut.shouldShowAtLaunch(0, false))
        assertTrue(sut.shouldShowAtLaunch(week * 2, false))
    }

    @Test
    fun `should skip a launch with another startup dialog without losing eligibility`() {
        repeat(3) { sut.recordCompletedGame() }

        assertFalse(sut.shouldShowAtLaunch(100, true))
        assertTrue(ReviewPromptPreferences(store).shouldShowAtLaunch(100, false))
    }

    @Test
    fun `should allow review to replace a repeatedly cancelled promotion on a later launch`() {
        repeat(3) { sut.recordCompletedGame() }
        assertFalse(sut.shouldShowAtLaunch(100, false, blackFridayVisible = true))

        sut.markPromotionDismissed()
        val restored = ReviewPromptPreferences(store)
        assertTrue(restored.shouldShowAtLaunch(100, false, blackFridayVisible = true))
        assertFalse(restored.shouldShowAtLaunch(100, true, blackFridayVisible = true))

        restored.postpone(100)
        assertFalse(restored.shouldShowAtLaunch(100 + week, false, blackFridayVisible = true))
    }
}

private class FakePreferenceStore : PreferenceStore {
    private val strings = mutableMapOf<String, String>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun contains(key: String) = key in strings || key in booleans
    override fun getString(key: String) = strings[key]
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getBoolean(key: String) = booleans[key] ?: false
    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
}
