package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class RemoteCodeTest {
    @Test
    fun `should accept enabled all-category code before expiry`() {
        // Given
        val code = RemoteCode(
            value = "ALL",
            categoryIds = emptySet(),
            enabled = true,
            unlockAllCategories = true,
            expiresAtMillis = 2_000,
        )

        // When / Then
        assertTrue(code.isValidAt(1_999))
        assertTrue(code.entitledCategoryIds(setOf("animals", "food")) == setOf("animals", "food"))
    }

    @Test
    fun `should reject expired disabled and categoryless codes`() {
        // Given
        val expired = RemoteCode("EXPIRED", emptySet(), expiresAtMillis = 1_000)
        val disabled = RemoteCode("DISABLED", setOf("animals"), enabled = false)
        val categoryless = RemoteCode("EMPTY", emptySet())

        // When / Then
        assertFalse(expired.isValidAt(1_000))
        assertFalse(disabled.isValidAt(0))
        assertFalse(categoryless.isValidAt(0))
    }
}
