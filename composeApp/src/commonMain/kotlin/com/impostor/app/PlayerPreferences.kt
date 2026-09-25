package com.impostor.app

import com.impostor.domain.Player
import com.impostor.domain.PlayerNamesCodec

internal expect fun readSavedPlayerNames(): String?

internal expect fun writeSavedPlayerNames(serializedPlayers: String)

private val defaultPlayers = (1..3).map { Player("p$it", "Gracz $it") }

internal fun loadPlayers(): List<Player> {
    val savedNames = readSavedPlayerNames()?.let(PlayerNamesCodec::decode)
    return savedNames?.mapIndexed { index, name -> Player("p${index + 1}", name) } ?: defaultPlayers
}

internal fun savePlayers(players: List<Player>) {
    writeSavedPlayerNames(PlayerNamesCodec.encode(players))
}