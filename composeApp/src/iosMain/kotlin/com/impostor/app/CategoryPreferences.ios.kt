package com.impostor.app

import platform.Foundation.NSUserDefaults

private const val SELECTED_CATEGORY_KEY = "selectedCategoryId"

internal actual fun readSelectedCategoryId(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(SELECTED_CATEGORY_KEY)

internal actual fun writeSelectedCategoryId(categoryId: String) {
    NSUserDefaults.standardUserDefaults.setObject(categoryId, forKey = SELECTED_CATEGORY_KEY)
}