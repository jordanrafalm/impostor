package com.impostor.app

import kotlin.test.Test
import kotlin.test.assertEquals

class PartyReminderScheduleTest {
    @Test
    fun `should pick first Friday in odd months and first Saturday in even months`() {
        // Given / When / Then
        assertEquals(2, partyReminderDay(2026, 1)) // Fri 2 Jan 2026
        assertEquals(7, partyReminderDay(2026, 2)) // Sat 7 Feb 2026
        assertEquals(6, partyReminderDay(2026, 11)) // Fri 6 Nov 2026
        assertEquals(5, partyReminderDay(2026, 12)) // Sat 5 Dec 2026
    }

    @Test
    fun `should start with this month when reminder is still ahead`() {
        // When
        val dates = upcomingPartyReminders(2026, 11, 6, 21, count = 1)

        // Then
        assertEquals(listOf(PartyReminderDate(2026, 11, 6)), dates)
    }

    @Test
    fun `should roll over to next year when reminder already passed in December`() {
        // When
        val dates = upcomingPartyReminders(2026, 12, 5, 22, count = 2)

        // Then
        assertEquals(
            listOf(PartyReminderDate(2027, 1, 1), PartyReminderDate(2027, 2, 6)),
            dates,
        )
    }
}
