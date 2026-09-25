package com.impostor.domain

object PlayerNamesCodec {
    fun encode(players: List<Player>): String = buildString {
        players.forEach { player ->
            val name = normalizePlayerName(player.displayName)
            append(name.length)
            append(':')
            append(name)
        }
    }

    fun decode(serialized: String): List<String>? {
        if (serialized.isEmpty()) return null

        val names = mutableListOf<String>()
        var index = 0
        while (index < serialized.length) {
            val separatorIndex = serialized.indexOf(':', startIndex = index)
            if (separatorIndex <= index) return null

            val length = serialized.substring(index, separatorIndex).toIntOrNull() ?: return null
            if (length < 0) return null
            val nameStart = separatorIndex + 1
            val nameEnd = nameStart + length
            if (nameEnd > serialized.length) return null

            val name = normalizePlayerName(serialized.substring(nameStart, nameEnd))
            if (name.isEmpty() || names.any { it.equals(name, ignoreCase = true) }) return null
            names += name
            index = nameEnd
        }

        return names.takeIf { it.size >= 3 }
    }
}