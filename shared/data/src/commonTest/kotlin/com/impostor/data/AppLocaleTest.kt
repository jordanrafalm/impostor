package com.impostor.data

import kotlin.test.Test
import kotlin.test.assertEquals

internal class AppLocaleTest {
    @Test
    fun `should resolve Polish when it is the first supported language`() {
        // Given
        val preferredTags = listOf("pl-PL", "en-US")

        // When
        val locale = resolveAppLocale(preferredTags)

        // Then
        assertEquals(AppLocale.POLISH, locale)
    }

    @Test
    fun `should resolve English language regions`() {
        // Given
        val preferredTags = listOf("en-GB")

        // When
        val locale = resolveAppLocale(preferredTags)

        // Then
        assertEquals(AppLocale.ENGLISH, locale)
    }

    @Test
    fun `should choose the first supported language after unsupported preferences`() {
        // Given
        val preferredTags = listOf("de-DE", "pl-PL", "en-US")

        // When
        val locale = resolveAppLocale(preferredTags)

        // Then
        assertEquals(AppLocale.POLISH, locale)
    }

    @Test
    fun `should fall back to English when preferences are unsupported or empty`() {
        // Given
        val unsupportedTags = listOf("de-DE", "fr-FR")

        // When
        val unsupportedLocale = resolveAppLocale(unsupportedTags)
        val emptyLocale = resolveAppLocale(emptyList())

        // Then
        assertEquals(AppLocale.ENGLISH, unsupportedLocale)
        assertEquals(AppLocale.ENGLISH, emptyLocale)
    }

    @Test
    fun `should resolve Ukrainian before later supported languages`() {
        // Given
        val preferredTags = listOf("de-DE", "uk-UA", "en-US")

        // When
        val locale = resolveAppLocale(preferredTags)

        // Then
        assertEquals(AppLocale.UKRAINIAN, locale)
    }
}
