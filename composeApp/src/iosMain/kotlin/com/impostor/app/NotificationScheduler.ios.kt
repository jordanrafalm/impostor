package com.impostor.app

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val PARTY_REMINDER_TITLE = "Impreza?"
private const val PARTY_REMINDER_BODY = "Zagraj ze znajomymi w Impostora!"
private const val PARTY_REMINDER_COUNT = 60

private class IosNotificationScheduler : NotificationScheduler {
    override fun requestPermissionAndSchedulePartyReminder() {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
        ) { granted, _ ->
            if (granted) scheduleMonthlyPartyReminder(center)
        }
    }
}

private fun scheduleMonthlyPartyReminder(center: UNUserNotificationCenter) {
    val calendar = NSCalendar.currentCalendar
    val now = NSDate()
    val current = calendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour,
        fromDate = now,
    )
    val identifiers = (0 until PARTY_REMINDER_COUNT).map { "party-reminder-$it" }
    center.removePendingNotificationRequestsWithIdentifiers(identifiers)

    var year = current.year.toInt()
    var month = current.month.toInt()
    if (!isPartyReminderStillUpcoming(
            calendar,
            year,
            month,
            current.day.toInt(),
            current.hour.toInt(),
        )
    ) {
        month += 1
        if (month == 13) {
            month = 1
            year += 1
        }
    }
    repeat(PARTY_REMINDER_COUNT) { index ->
        val date = partyReminderDate(calendar, year, month)
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date,
        )
        val content = UNMutableNotificationContent()
        content.setTitle(PARTY_REMINDER_TITLE)
        content.setBody(PARTY_REMINDER_BODY)
        content.setSound(UNNotificationSound.defaultSound)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "party-reminder-$index",
            content = content,
            trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = components,
                repeats = false,
            ),
        )
        center.addNotificationRequest(request, withCompletionHandler = null)
        month += 1
        if (month == 13) {
            month = 1
            year += 1
        }
    }
}

private fun isPartyReminderStillUpcoming(
    calendar: NSCalendar,
    year: Int,
    month: Int,
    currentDay: Int,
    currentHour: Int,
): Boolean = partyReminderDay(calendar, year, month) > currentDay ||
    (partyReminderDay(calendar, year, month) == currentDay && currentHour < 22)

private fun partyReminderDate(calendar: NSCalendar, year: Int, month: Int): NSDate {
    val firstDay = NSDateComponents().apply {
        this.year = year.toLong()
        this.month = month.toLong()
        day = 1
        hour = 22
        minute = 0
    }
    val firstDate = calendar.dateFromComponents(firstDay) ?: NSDate()
    val day = partyReminderDay(calendar, year, month)
    return calendar.dateFromComponents(firstDay.apply { this.day = day.toLong() }) ?: firstDate
}

private fun partyReminderDay(calendar: NSCalendar, year: Int, month: Int): Int {
    val firstDay = NSDateComponents().apply {
        this.year = year.toLong()
        this.month = month.toLong()
        day = 1
    }
    val firstDate = calendar.dateFromComponents(firstDay) ?: NSDate()
    val targetWeekday = if (month % 2 == 1) 6 else 7
    val weekday = calendar.component(NSCalendarUnitWeekday, fromDate = firstDate).toInt()
    return 1 + (targetWeekday - weekday + 7) % 7
}

actual fun platformNotificationScheduler(): NotificationScheduler = IosNotificationScheduler()
