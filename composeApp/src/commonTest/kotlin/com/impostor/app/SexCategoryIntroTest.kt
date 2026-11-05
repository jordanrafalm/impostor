package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SexCategoryIntroTest {
    @Test
    fun `should retain fresh installation eligibility after app preferences are created`() {
        // Given
        val storedEligibility = false
        val laterDetectedExistingInstallation = true

        // When
        val eligibility = resolveSexCategoryIntroEligibility(storedEligibility, laterDetectedExistingInstallation)

        // Then
        assertFalse(eligibility)
    }

    @Test
    fun `should show intro to an existing installation when not previously dismissed`() {
        // Given
        val isExistingInstallation = true
        val hasDismissedIntro = false

        // When
        val shouldShow = shouldShowSexCategoryIntro(isExistingInstallation, hasDismissedIntro)

        // Then
        assertTrue(shouldShow)
    }

    @Test
    fun `should not show intro on a fresh installation`() {
        // Given
        val isExistingInstallation = false
        val hasDismissedIntro = false

        // When
        val shouldShow = shouldShowSexCategoryIntro(isExistingInstallation, hasDismissedIntro)

        // Then
        assertFalse(shouldShow)
    }

    @Test
    fun `should not show intro again after any dismissal is persisted`() {
        // Given
        val isExistingInstallation = true
        val hasDismissedIntro = true

        // When
        val shouldShow = shouldShowSexCategoryIntro(isExistingInstallation, hasDismissedIntro)

        // Then
        assertFalse(shouldShow)
    }
}
