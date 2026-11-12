package com.impostor.app

import com.impostor.domain.Assignment
import com.impostor.domain.GameSession
import com.impostor.domain.Prompt
import com.impostor.domain.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppStateSaveableTest {
    @Test
    fun `game session save state restores the same round and reveal index`() {
        val session = GameSession(
            sessionId = "session-1",
            prompt = Prompt("prompt-1", "places", "Wieliczka", "Sól"),
            assignments = listOf(
                Assignment("player-1", Role.AGENT),
                Assignment("player-2", Role.IMPOSTOR),
            ),
            starterPlayerId = "player-2",
            revealIndex = 1,
            hintsEnabled = true,
        )

        assertEquals(session, restoreGameSession(saveGameSession(session)))
    }

    @Test
    fun `invalid game session save state is rejected`() {
        val saved = saveGameSession(
            GameSession(
                sessionId = "session-1",
                prompt = Prompt("prompt-1", "places", "Wieliczka"),
                assignments = listOf(Assignment("player-1", Role.AGENT)),
                starterPlayerId = "player-1",
            ),
        ).toMutableList()
        saved[6] = "1"

        assertNull(restoreGameSession(saved))
    }

    @Test
    fun `should restore active routes with the same game session`() {
        val session = GameSession(
            sessionId = "session-2",
            prompt = Prompt("prompt-2", "animals", "Otter", "River"),
            assignments = listOf(Assignment("p1", Role.AGENT), Assignment("p2", Role.IMPOSTOR)),
            starterPlayerId = "p2",
            revealIndex = 1,
            hintsEnabled = true,
        )
        val routes = listOf(Route.Reveal(session), Route.GameStart(session))

        routes.forEach { assertEquals(it, restoreRoute(saveRoute(it))) }
        assertNull(restoreRoute(listOf("reveal", "invalid")))
    }
}
