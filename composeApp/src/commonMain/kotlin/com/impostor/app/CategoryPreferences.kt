package com.impostor.app

internal fun readSelectedCategoryId(): String? =
    platformPreferenceStore().getString(PreferenceKeys.SELECTED_CATEGORY)

internal fun writeSelectedCategoryId(categoryId: String) {
    platformPreferenceStore().putString(PreferenceKeys.SELECTED_CATEGORY, categoryId)
}