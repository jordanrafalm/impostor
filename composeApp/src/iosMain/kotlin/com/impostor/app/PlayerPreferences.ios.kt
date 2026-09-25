package com.impostor.app

import platform.Foundation.NSUserDefaults

private const val SAVED_PLAYER_NAMES_KEY = "savedPlayerNames"

internal actual fun readSavedPlayerNames(): String? =
    NSUserDefaults.standardUserDefaults.stringForKey(SAVED_PLAYER_NAMES_KEY)

internal actual fun writeSavedPlayerNames(serializedPlayers: String) {
    NSUserDefaults.standardUserDefaults.setObject(serializedPlayers, forKey = SAVED_PLAYER_NAMES_KEY)
}