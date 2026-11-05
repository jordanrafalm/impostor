package com.impostor.app

private const val EXISTING = "existing"
private const val FRESH = "fresh"

internal fun isExistingInstallation(): Boolean = platformPreferenceStore().let {
    it.contains(PreferenceKeys.SAVED_PLAYER_NAMES) || it.contains(PreferenceKeys.SELECTED_CATEGORY)
}

internal fun readSexCategoryIntroEligibility(): Boolean? =
    when (platformPreferenceStore().getString(PreferenceKeys.SEX_INTRO_ELIGIBILITY)) {
        EXISTING -> true
        FRESH -> false
        else -> null
    }

internal fun writeSexCategoryIntroEligibility(isEligible: Boolean) {
    platformPreferenceStore().putString(PreferenceKeys.SEX_INTRO_ELIGIBILITY, if (isEligible) EXISTING else FRESH)
}

internal fun hasDismissedSexCategoryIntro(): Boolean =
    platformPreferenceStore().getBoolean(PreferenceKeys.SEX_INTRO_DISMISSED)

internal fun markSexCategoryIntroDismissed() {
    platformPreferenceStore().putBoolean(PreferenceKeys.SEX_INTRO_DISMISSED, true)
}

internal fun resolveSexCategoryIntroEligibility(
    storedEligibility: Boolean?,
    detectedExistingInstallation: Boolean,
): Boolean = storedEligibility ?: detectedExistingInstallation

internal fun shouldShowSexCategoryIntro(
    isExistingInstallation: Boolean,
    hasDismissedIntro: Boolean,
): Boolean = isExistingInstallation && !hasDismissedIntro
