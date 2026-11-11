package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals

internal class HomeHolidaySequenceTest {
    @Test
    fun `should progress from button exit through reindeer and button return`() {
        // Given
        val initialPhase = HomeHolidaySequencePhase.IDLE

        // When
        val exiting = reduceHomeHolidaySequence(initialPhase, HomeHolidaySequenceEvent.START)
        val reindeer = reduceHomeHolidaySequence(exiting, HomeHolidaySequenceEvent.BUTTONS_EXITED)
        val entering = reduceHomeHolidaySequence(reindeer, HomeHolidaySequenceEvent.REINDEER_FINISHED)
        val returned = reduceHomeHolidaySequence(entering, HomeHolidaySequenceEvent.BUTTONS_RETURNED)

        // Then
        assertEquals(HomeHolidaySequencePhase.BUTTONS_EXITING, exiting)
        assertEquals(HomeHolidaySequencePhase.REINDEER, reindeer)
        assertEquals(HomeHolidaySequencePhase.BUTTONS_ENTERING, entering)
        assertEquals(HomeHolidaySequencePhase.IDLE, returned)
    }

    @Test
    fun `should restore idle phase when sequence is interrupted`() {
        // Given
        val activePhases = listOf(
            HomeHolidaySequencePhase.BUTTONS_EXITING,
            HomeHolidaySequencePhase.REINDEER,
            HomeHolidaySequencePhase.BUTTONS_ENTERING,
        )

        // When
        val restoredPhases = activePhases.map {
            reduceHomeHolidaySequence(it, HomeHolidaySequenceEvent.INTERRUPTED)
        }

        // Then
        assertEquals(List(activePhases.size) { HomeHolidaySequencePhase.IDLE }, restoredPhases)
    }
}
