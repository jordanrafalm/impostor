package com.impostor.app

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val GAMES_BEFORE_REVIEW = 1
private const val REVIEW_COOLDOWN_MILLIS = 7L * 24 * 60 * 60 * 1000

internal expect fun reviewStoreUrl(): String

@OptIn(ExperimentalTime::class)
internal fun currentReviewTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

internal class ReviewPromptPreferences(private val store: PreferenceStore) {
    fun hasOpenedStore(): Boolean = store.getBoolean(PreferenceKeys.REVIEW_STORE_OPENED)

    fun recordCompletedGame() {
        if (hasOpenedStore()) return
        val games = (store.getString(PreferenceKeys.REVIEW_COMPLETED_GAMES)?.toIntOrNull() ?: 0)
            .coerceIn(0, GAMES_BEFORE_REVIEW)
        val completedGames = (games + 1).coerceAtMost(GAMES_BEFORE_REVIEW)
        store.putString(PreferenceKeys.REVIEW_COMPLETED_GAMES, completedGames.toString())
    }

    fun shouldShowAtLaunch(
        nowMillis: Long,
        hasOtherStartupDialog: Boolean,
        blackFridayVisible: Boolean = false,
    ): Boolean {
        if (hasOtherStartupDialog || hasOpenedStore() ||
            (blackFridayVisible && !store.getBoolean(PreferenceKeys.REVIEW_PROMOTION_DISMISSED))
        ) return false
        val completedGames = (store.getString(PreferenceKeys.REVIEW_COMPLETED_GAMES)?.toIntOrNull() ?: 0)
            .coerceIn(0, GAMES_BEFORE_REVIEW)
        val deferredAt = store.getString(PreferenceKeys.REVIEW_DEFERRED_AT)?.toLongOrNull()
        return completedGames >= GAMES_BEFORE_REVIEW &&
            (deferredAt == null || (nowMillis >= deferredAt && nowMillis - deferredAt >= REVIEW_COOLDOWN_MILLIS))
    }

    fun postpone(nowMillis: Long) {
        store.putString(PreferenceKeys.REVIEW_DEFERRED_AT, nowMillis.toString())
        store.putString(PreferenceKeys.REVIEW_COMPLETED_GAMES, "0")
    }

    fun markStoreOpened() {
        store.putBoolean(PreferenceKeys.REVIEW_STORE_OPENED, true)
    }

    fun markPromotionDismissed() {
        store.putBoolean(PreferenceKeys.REVIEW_PROMOTION_DISMISSED, true)
    }
}
