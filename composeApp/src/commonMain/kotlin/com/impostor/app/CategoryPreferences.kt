package com.impostor.app

import com.impostor.domain.Entitlement
import com.impostor.domain.TrialState
import com.impostor.domain.canStartGame
import com.impostor.domain.requiresTrialGame

internal fun allCategoriesAvailable(debugUnlockCategories: Boolean, trial: TrialState, entitlement: Entitlement): Boolean =
    debugUnlockCategories || trial.canStartGame(entitlement)

internal fun shouldRecordCategoryTrial(
    debugUnlockCategories: Boolean,
    categoryIds: Set<String>,
    entitlement: Entitlement,
): Boolean = !debugUnlockCategories && requiresTrialGame(categoryIds, entitlement)

internal fun readSelectedCategoryIds(): Set<String>? {
    val store = platformPreferenceStore()
    return decodeSelectedCategoryIds(
        encodedIds = store.getString(PreferenceKeys.SELECTED_CATEGORY_IDS),
        legacyId = store.getString(PreferenceKeys.SELECTED_CATEGORY),
    )
}

internal fun writeSelectedCategoryIds(categoryIds: Set<String>) {
    platformPreferenceStore().putString(
        PreferenceKeys.SELECTED_CATEGORY_IDS,
        encodeSelectedCategoryIds(categoryIds),
    )
}

internal fun encodeSelectedCategoryIds(categoryIds: Set<String>): String =
    categoryIds.sorted().joinToString(",")

internal fun decodeSelectedCategoryIds(encodedIds: String?, legacyId: String?): Set<String>? =
    encodedIds?.split(",")?.filter(String::isNotBlank)?.toSet()
        ?: legacyId?.let(::setOf)

internal fun toggleCategorySelection(selected: Set<String>, categoryId: String): Set<String> =
    if (categoryId in selected) selected - categoryId else selected + categoryId