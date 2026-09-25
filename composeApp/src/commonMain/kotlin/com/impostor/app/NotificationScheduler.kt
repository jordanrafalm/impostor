package com.impostor.app

/** Schedules the recurring invitation to play with friends. */
interface NotificationScheduler {
    fun requestPermissionAndSchedulePartyReminder()
}

expect fun platformNotificationScheduler(): NotificationScheduler
