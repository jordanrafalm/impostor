package com.impostor.app

import android.content.Context

private const val PREFERENCES_NAME = "impostor_preferences"
private const val SAVED_PLAYER_NAMES_KEY = "savedPlayerNames"

internal actual fun readSavedPlayerNames(): String? =
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.getString(SAVED_PLAYER_NAMES_KEY, null)

internal actual fun writeSavedPlayerNames(serializedPlayers: String) {
    MainActivity.currentActivity
        ?.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        ?.edit()
        ?.putString(SAVED_PLAYER_NAMES_KEY, serializedPlayers)
        ?.apply()
}