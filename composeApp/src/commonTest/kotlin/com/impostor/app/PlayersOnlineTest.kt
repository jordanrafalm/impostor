package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals

class PlayersOnlineTest {
    private fun m(dow: Int, hour: Int) = LocalMoment(2026, 11, 5, hour, dow)

    @Test fun rangesFollowSchedule() {
        assertEquals(2..5, playersOnlineRange(m(1, 0)))
        assertEquals(4..7, playersOnlineRange(m(4, 16)))
        assertEquals(2..7, playersOnlineRange(m(5, 15)))
        assertEquals(10..17, playersOnlineRange(m(5, 16)))
        assertEquals(4..7, playersOnlineRange(m(6, 14)))
        assertEquals(7..17, playersOnlineRange(m(7, 15)))
    }

    @Test fun sortsByPopularity() {
        val ids = listOf("x", "places", "adult", "animals", "y")
        assertEquals(listOf("adult", "animals", "places", "x", "y"), sortByPopularity(ids) { it })
    }
}
