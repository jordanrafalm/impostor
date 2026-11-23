package com.impostor.app

/** Schedules the recurring invitation to play with friends. */
interface NotificationScheduler {
    fun requestPermissionAndSchedulePartyReminder(
        onPermissionPromptStatusChanged: (NotificationPermissionPromptStatus) -> Unit,
    )
}

enum class NotificationPermissionPromptStatus {
    PENDING_CHECK,
    PROMPT_REQUIRED_OR_IN_PROGRESS,
    NO_PROMPT_REQUIRED,
}

expect fun platformNotificationScheduler(): NotificationScheduler
