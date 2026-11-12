package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryPreferencesTest {
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
