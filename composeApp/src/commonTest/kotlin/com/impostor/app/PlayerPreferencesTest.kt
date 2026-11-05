package com.impostor.app

import com.impostor.domain.Player
import kotlin.test.Test
import kotlin.test.assertEquals

internal class PlayerPreferencesTest {
    @Test
    fun `should skip an existing ID after a player was removed`() {
        val players = listOf(
            Player("p1", "Player 1"),
            Player("p3", "Player 3"),
            Player("p4", "Player 4"),
        )

        assertEquals("p5", nextPlayerId(players))
    }

    @Test
    fun `should assign the next sequential ID when IDs are contiguous`() {
        val players = (1..3).map { Player("p$it", "Player $it") }

        assertEquals("p4", nextPlayerId(players))
    }
}
