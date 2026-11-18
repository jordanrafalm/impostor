package com.impostor.app

import com.impostor.domain.Entitlement
import com.impostor.domain.EntitlementSource
import com.impostor.domain.SubscriptionState
import com.impostor.domain.TrialState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryPreferencesTest {
    @Test
    fun `debug access unlocks categories without consuming trial games`() {
        val locked = Entitlement(state = SubscriptionState.LOCKED, source = EntitlementSource.LOCAL_CACHE)
        val exhausted = TrialState(startedGames = 5)
        assertTrue(allCategoriesAvailable(true, exhausted, locked))
        assertFalse(shouldRecordCategoryTrial(true, setOf("adult"), locked))
        assertFalse(allCategoriesAvailable(false, exhausted, locked))
        assertTrue(shouldRecordCategoryTrial(false, setOf("adult"), locked))
        assertFalse(shouldRecordCategoryTrial(false, setOf("animals"), locked))
        assertTrue(allCategoriesAvailable(false, TrialState(startedGames = 0), locked))
        val premium = Entitlement(state = SubscriptionState.ACTIVE, source = EntitlementSource.STORE)
        assertTrue(allCategoriesAvailable(false, exhausted, premium))
        assertFalse(shouldRecordCategoryTrial(false, setOf("adult"), premium))
    }

    @Test
    fun `should restore all saved category ids`() {
        assertEquals(
            setOf("animals", "places", "sports"),
            decodeSelectedCategoryIds(
                encodeSelectedCategoryIds(setOf("sports", "animals", "places")),
                legacyId = null,
            ),
        )
    }

    @Test
    fun `should migrate a legacy single category preference`() {
        assertEquals(setOf("places"), decodeSelectedCategoryIds(encodedIds = null, legacyId = "places"))
    }

    @Test
    fun `should preserve an intentionally empty category set`() {
        assertEquals(emptySet(), decodeSelectedCategoryIds(encodedIds = "", legacyId = "places"))
    }

    @Test
    fun `should add and remove categories without replacing the selected set`() {
        val selected = setOf("animals", "places")

        assertEquals(setOf("animals", "places", "sports"), toggleCategorySelection(selected, "sports"))
        assertEquals(setOf("places"), toggleCategorySelection(selected, "animals"))
    }
}
