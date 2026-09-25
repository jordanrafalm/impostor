package com.impostor.data

import com.impostor.domain.isValidAt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PromoCodeMappingTest {
    @Test
    fun `should map enabled all-category document`() {
        // Given
        val document = PromoCodeDocument(
            enabled = true,
            unlockAllCategories = true,
            categoryIds = listOf("animals", "food"),
        )

        // When
        val code = document.toRemoteCode("WINTER")

        // Then
        assertEquals("WINTER", code.value)
        assertTrue(code.enabled)
        assertTrue(code.unlockAllCategories)
        assertEquals(setOf("animals", "food"), code.categoryIds)
    }

    @Test
    fun `should reject disabled and structurally invalid codes`() {
        // Given
        val disabled = PromoCodeDocument(enabled = false, categoryIds = listOf("animals"))
        val withoutEntitlement = PromoCodeDocument(enabled = true)

        // When / Then
        assertFalse(disabled.toRemoteCode("DISABLED").isValidAt(0))
        assertFalse(withoutEntitlement.toRemoteCode("EMPTY").isValidAt(0))
    }
}
