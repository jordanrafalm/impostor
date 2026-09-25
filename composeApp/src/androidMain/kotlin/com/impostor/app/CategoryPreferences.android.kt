package com.impostor.app

import android.content.Context

private const val PREFERENCES_NAME = "impostor_preferences"
private const val SELECTED_CATEGORY_KEY = "selectedCategoryId"

internal actual fun readSelectedCategoryId(): String? =
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.getString(SELECTED_CATEGORY_KEY, null)

internal actual fun writeSelectedCategoryId(categoryId: String) {
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(SELECTED_CATEGORY_KEY, categoryId)
        ?.apply()
}