package com.impostor.app

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSBundle
import platform.Foundation.NSNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

private const val PARTY_REMINDER_COUNT = 60

private class IosNotificationScheduler : NotificationScheduler {
    init {
        NSNotificationCenter.defaultCenter.addObserverForName(
            "UIApplicationDidBecomeActiveNotification",
            null,
            null,
        ) {
            scheduleMonthlyPartyReminder(UNUserNotificationCenter.currentNotificationCenter())
        }
    }

    override fun requestPermissionAndSchedulePartyReminder(
        onPermissionPromptStatusChanged: (NotificationPermissionPromptStatus) -> Unit,
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.PENDING_CHECK)
        center.getNotificationSettingsWithCompletionHandler { settings ->
            when (settings?.authorizationStatus) {
                UNAuthorizationStatusNotDetermined -> {
                    onPermissionPromptStatusChanged(
                        NotificationPermissionPromptStatus.PROMPT_REQUIRED_OR_IN_PROGRESS,
                    )
                    center.requestAuthorizationWithOptions(
                        UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound,
                    ) { granted, _ ->
                        if (granted) scheduleMonthlyPartyReminder(center)
                        onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
                    }
                }
                UNAuthorizationStatusAuthorized,
                UNAuthorizationStatusProvisional,
                UNAuthorizationStatusEphemeral -> {
                    scheduleMonthlyPartyReminder(center)
                    onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
                }
                UNAuthorizationStatusDenied -> {
                    onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
                }
                else -> onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
            }
        }
    }
}

private fun scheduleMonthlyPartyReminder(center: UNUserNotificationCenter) {
    val current = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or NSCalendarUnitHour,
        fromDate = NSDate(),
    )
    center.removePendingNotificationRequestsWithIdentifiers((0 until PARTY_REMINDER_COUNT).map { "party-reminder-$it" })

    upcomingPartyReminders(
        year = current.year.toInt(),
        month = current.month.toInt(),
        day = current.day.toInt(),
        hour = current.hour.toInt(),
        count = PARTY_REMINDER_COUNT,
    ).forEachIndexed { index, date ->
        val components = NSDateComponents().apply {
            year = date.year.toLong()
            month = date.month.toLong()
            day = date.day.toLong()
            hour = PARTY_REMINDER_HOUR.toLong()
            minute = 0
        }
        val content = UNMutableNotificationContent()
        content.setTitle(localizedNotificationString("party_reminder_title"))
        content.setBody(localizedNotificationString("party_reminder_body"))
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
    }
}

private val iosNotificationScheduler: NotificationScheduler by lazy { IosNotificationScheduler() }

private fun localizedNotificationString(key: String): String =
    NSBundle.mainBundle.localizedStringForKey(key, value = null, table = null)

actual fun platformNotificationScheduler(): NotificationScheduler = iosNotificationScheduler
