package com.impostor.app

internal enum class HomeHolidaySequencePhase {
    IDLE,
    BUTTONS_EXITING,
    REINDEER,
    BUTTONS_ENTERING,
}

internal enum class HomeHolidaySequenceEvent {
    START,
    BUTTONS_EXITED,
    REINDEER_FINISHED,
    BUTTONS_RETURNED,
    INTERRUPTED,
}

internal fun reduceHomeHolidaySequence(
    phase: HomeHolidaySequencePhase,
    event: HomeHolidaySequenceEvent,
): HomeHolidaySequencePhase = when (event) {
    HomeHolidaySequenceEvent.START ->
        if (phase == HomeHolidaySequencePhase.IDLE) HomeHolidaySequencePhase.BUTTONS_EXITING else phase
    HomeHolidaySequenceEvent.BUTTONS_EXITED ->
        if (phase == HomeHolidaySequencePhase.BUTTONS_EXITING) HomeHolidaySequencePhase.REINDEER else phase
    HomeHolidaySequenceEvent.REINDEER_FINISHED ->
        if (phase == HomeHolidaySequencePhase.REINDEER) HomeHolidaySequencePhase.BUTTONS_ENTERING else phase
    HomeHolidaySequenceEvent.BUTTONS_RETURNED ->
        if (phase == HomeHolidaySequencePhase.BUTTONS_ENTERING) HomeHolidaySequencePhase.IDLE else phase
    HomeHolidaySequenceEvent.INTERRUPTED -> HomeHolidaySequencePhase.IDLE
}
