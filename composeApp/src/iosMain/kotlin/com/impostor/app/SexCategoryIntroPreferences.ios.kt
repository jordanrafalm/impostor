package com.impostor.app

import platform.Foundation.NSUserDefaults

private const val SEX_CATEGORY_INTRO_DISMISSED_KEY = "sexCategoryIntroDismissed"
private const val SEX_CATEGORY_INTRO_ELIGIBILITY_KEY = "sexCategoryIntroEligibility"

internal actual fun isExistingInstallation(): Boolean {
    val preferences = NSUserDefaults.standardUserDefaults
    return preferences.objectForKey("savedPlayerNames") != null ||
        preferences.objectForKey("selectedCategoryId") != null
}

internal actual fun readSexCategoryIntroEligibility(): Boolean? =
    when (NSUserDefaults.standardUserDefaults.stringForKey(SEX_CATEGORY_INTRO_ELIGIBILITY_KEY)) {
        "existing" -> true
        "fresh" -> false
        else -> null
    }

internal actual fun writeSexCategoryIntroEligibility(isEligible: Boolean) {
    NSUserDefaults.standardUserDefaults.setObject(
        if (isEligible) "existing" else "fresh",
        forKey = SEX_CATEGORY_INTRO_ELIGIBILITY_KEY,
    )
}

internal actual fun hasDismissedSexCategoryIntro(): Boolean =
    NSUserDefaults.standardUserDefaults.boolForKey(SEX_CATEGORY_INTRO_DISMISSED_KEY)

internal actual fun markSexCategoryIntroDismissed() {
    NSUserDefaults.standardUserDefaults.setBool(true, forKey = SEX_CATEGORY_INTRO_DISMISSED_KEY)
}
