package com.impostor.app

internal const val PARTY_REMINDER_HOUR = 22

internal data class PartyReminderDate(val year: Int, val month: Int, val day: Int)

/** Day of month (month is 1-based) of the first Friday in odd months or Saturday in even months. */
internal fun partyReminderDay(year: Int, month: Int): Int {
    val targetWeekday = if (month % 2 == 1) 5 else 6
    return 1 + (targetWeekday - dayOfWeek(year, month, 1) + 7) % 7
}

/**
 * Upcoming reminder dates, each at [PARTY_REMINDER_HOUR]:00, starting with the first one that
 * is still ahead of the given current moment.
 */
internal fun upcomingPartyReminders(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    count: Int,
): List<PartyReminderDate> {
    val currentDay = partyReminderDay(year, month)
    val startsThisMonth = currentDay > day || (currentDay == day && hour < PARTY_REMINDER_HOUR)
    val startIndex = year * 12 + (month - 1) + if (startsThisMonth) 0 else 1
    return (0 until count).map { offset ->
        val index = startIndex + offset
        val reminderYear = index / 12
        val reminderMonth = index % 12 + 1
        PartyReminderDate(reminderYear, reminderMonth, partyReminderDay(reminderYear, reminderMonth))
    }
}

// Sakamoto's algorithm; 0 = Sunday.
private fun dayOfWeek(year: Int, month: Int, day: Int): Int {
    val offsets = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
    val adjustedYear = if (month < 3) year - 1 else year
    return (adjustedYear + adjustedYear / 4 - adjustedYear / 100 + adjustedYear / 400 + offsets[month - 1] + day) % 7
}
