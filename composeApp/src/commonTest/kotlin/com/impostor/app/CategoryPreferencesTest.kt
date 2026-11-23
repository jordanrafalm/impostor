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
    fun `should decode all saved category ids before catalog migration`() {
        assertEquals(
            setOf("animals", "places", "sports"),
            decodeSelectedCategoryIds(
                encodeSelectedCategoryIds(setOf("sports", "animals", "places")),
                legacyId = null,
            ),
        )
    }

    @Test
    fun `should migrate multiple saved categories using catalog order and ignore invalid ids`() {
        val decoded = decodeSelectedCategoryIds(
            encodedIds = "places,unknown,sports",
            legacyId = null,
        )

        assertEquals(
            setOf("sports"),
            normalizeSelectedCategoryIds(
                selectedCategoryIds = decoded.orEmpty(),
                categoryIdsInCatalogOrder = listOf("sports", "animals", "places"),
            ),
        )
    }

    @Test
    fun `should preserve an empty selection and default invalid selections to animals`() {
        val catalogOrder = listOf("sports", "animals", "places")

        assertEquals(emptySet(), normalizeSelectedCategoryIds(emptySet(), catalogOrder))
        assertEquals(setOf("animals"), normalizeSelectedCategoryIds(setOf("unknown"), catalogOrder))
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
    fun `should select one category or clear it when tapped again`() {
        assertEquals(setOf("sports"), toggleCategorySelection(setOf("animals", "places"), "sports"))
        assertEquals(emptySet(), toggleCategorySelection(setOf("animals"), "animals"))
    }
}
