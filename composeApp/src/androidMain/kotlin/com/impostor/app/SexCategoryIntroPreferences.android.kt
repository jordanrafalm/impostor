package com.impostor.app

import android.content.Context

private const val PREFERENCES_NAME = "impostor_preferences"
private const val SEX_CATEGORY_INTRO_ELIGIBILITY_KEY = "sexCategoryIntroEligibility"
private const val SEX_CATEGORY_INTRO_DISMISSED_KEY = "sexCategoryIntroDismissed"

internal actual fun isExistingInstallation(): Boolean =
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.let { it.contains("savedPlayerNames") || it.contains("selectedCategoryId") }
        ?: false

internal actual fun readSexCategoryIntroEligibility(): Boolean? =
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.getString(SEX_CATEGORY_INTRO_ELIGIBILITY_KEY, null)
        ?.let { value ->
            when (value) {
                "existing" -> true
                "fresh" -> false
                else -> null
            }
        }

internal actual fun writeSexCategoryIntroEligibility(isEligible: Boolean) {
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(SEX_CATEGORY_INTRO_ELIGIBILITY_KEY, if (isEligible) "existing" else "fresh")
        ?.apply()
}

internal actual fun hasDismissedSexCategoryIntro(): Boolean =
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.getBoolean(SEX_CATEGORY_INTRO_DISMISSED_KEY, false)
        ?: false

internal actual fun markSexCategoryIntroDismissed() {
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putBoolean(SEX_CATEGORY_INTRO_DISMISSED_KEY, true)
        ?.apply()
}
