package com.impostor.app

internal expect fun isExistingInstallation(): Boolean

internal expect fun readSexCategoryIntroEligibility(): Boolean?

internal expect fun writeSexCategoryIntroEligibility(isEligible: Boolean)

internal expect fun hasDismissedSexCategoryIntro(): Boolean

internal expect fun markSexCategoryIntroDismissed()

internal fun resolveSexCategoryIntroEligibility(
    storedEligibility: Boolean?,
    detectedExistingInstallation: Boolean,
): Boolean = storedEligibility ?: detectedExistingInstallation

internal fun shouldShowSexCategoryIntro(
    isExistingInstallation: Boolean,
    hasDismissedIntro: Boolean,
): Boolean = isExistingInstallation && !hasDismissedIntro
