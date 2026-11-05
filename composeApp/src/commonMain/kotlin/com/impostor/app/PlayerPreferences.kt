package com.impostor.app

import com.impostor.domain.Player
import com.impostor.domain.PlayerNamesCodec

internal expect fun readSavedPlayerNames(): String?

internal expect fun writeSavedPlayerNames(serializedPlayers: String)

internal fun defaultPlayerToken(playerNumber: Int): String = "__default_player_${playerNumber}__"

internal fun nextPlayerId(players: List<Player>): String {
    val existingIds = players.mapTo(mutableSetOf()) { it.id }
    var candidate = players.size + 1
    while ("p$candidate" in existingIds) candidate++
    return "p$candidate"
}

private val defaultPlayers = (1..3).map { Player("p$it", defaultPlayerToken(it)) }

internal fun loadPlayers(): List<Player> {
    val savedNames = readSavedPlayerNames()?.let(PlayerNamesCodec::decode)
    return savedNames?.mapIndexed { index, name -> Player("p${index + 1}", name) } ?: defaultPlayers
}

internal fun savePlayers(players: List<Player>) {
    writeSavedPlayerNames(PlayerNamesCodec.encode(players))
}