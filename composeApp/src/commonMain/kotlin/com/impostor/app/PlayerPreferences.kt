package com.impostor.app

import com.impostor.domain.Player
import com.impostor.domain.PlayerNamesCodec
import com.impostor.domain.normalizePlayerName

internal fun readSavedPlayerNames(): String? =
    platformPreferenceStore().getString(PreferenceKeys.SAVED_PLAYER_NAMES)

internal fun writeSavedPlayerNames(serializedPlayers: String) {
    platformPreferenceStore().putString(PreferenceKeys.SAVED_PLAYER_NAMES, serializedPlayers)
}

internal fun defaultPlayerToken(playerNumber: Int): String = "__default_player_${playerNumber}__"

internal fun nextPlayerId(players: List<Player>): String {
    val existingIds = players.mapTo(mutableSetOf()) { it.id }
    var candidate = players.size + 1
    while ("p$candidate" in existingIds) candidate++
    return "p$candidate"
}

internal fun appendPendingPlayer(players: List<Player>, pendingName: String): List<Player> {
    val name = normalizePlayerName(pendingName)
    if (name.isBlank()) return players
    return players + Player(nextPlayerId(players), name)
}

private val defaultPlayers = (1..3).map { Player("p$it", defaultPlayerToken(it)) }

internal fun loadPlayers(): List<Player> {
    val savedNames = readSavedPlayerNames()?.let(PlayerNamesCodec::decode)
    return savedNames?.mapIndexed { index, name -> Player("p${index + 1}", name) } ?: defaultPlayers
}

internal fun savePlayers(players: List<Player>) {
    writeSavedPlayerNames(PlayerNamesCodec.encode(players))
}