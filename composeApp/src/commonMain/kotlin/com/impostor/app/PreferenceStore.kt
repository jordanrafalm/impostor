package com.impostor.app

/** Minimal platform key-value storage; all keys and rules live in common code. */
internal interface PreferenceStore {
    fun contains(key: String): Boolean
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String): Boolean
    fun putBoolean(key: String, value: Boolean)
}

internal expect fun platformPreferenceStore(): PreferenceStore

internal object PreferenceKeys {
    const val SAVED_PLAYER_NAMES = "savedPlayerNames"
    const val SELECTED_CATEGORY_IDS = "selectedCategoryIds"
    const val SELECTED_CATEGORY = "selectedCategoryId"
    const val SEX_INTRO_ELIGIBILITY = "sexCategoryIntroEligibility"
    const val SEX_INTRO_DISMISSED = "sexCategoryIntroDismissed"
    const val REVIEW_COMPLETED_GAMES = "reviewCompletedGames"
    const val REVIEW_DEFERRED_AT = "reviewDeferredAt"
    const val REVIEW_STORE_OPENED = "reviewStoreOpened"
    const val REVIEW_PROMOTION_DISMISSED = "reviewPromotionDismissed"
}
