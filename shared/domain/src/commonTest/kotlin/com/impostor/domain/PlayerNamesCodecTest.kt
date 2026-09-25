package com.impostor.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class PlayerNamesCodecTest {
    @Test
    fun `should round trip normalized names`() {
        // Given
        val players = listOf(
            Player("p1", " ala "),
            Player("p2", "Bartek"),
            Player("p3", "zoe: test"),
        )

        // When
        val decoded = PlayerNamesCodec.decode(PlayerNamesCodec.encode(players))

        // Then
        assertEquals(listOf("Ala", "Bartek", "Zoe: test"), decoded)
    }

    @Test
    fun `should reject malformed or invalid player data`() {
        // Given
        val malformed = listOf("3:An", "x:Ala", "-1:Ala", "0:Ala3:Bob", "3:Ala3:ala")

        // When / Then
        malformed.forEach { serialized ->
            assertNull(PlayerNamesCodec.decode(serialized))
        }
    }
}