package com.impostor.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.rafal.impostorparty.R
import java.util.Calendar

private const val PARTY_REMINDER_CHANNEL_ID = "party_reminders"
private const val PARTY_REMINDER_NOTIFICATION_ID = 2200
private const val PARTY_REMINDER_REQUEST_CODE = 2200
private const val PARTY_REMINDER_PREFERENCES = "party_reminder_preferences"
private const val NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

internal class PartyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        showPartyReminder(context)
        AndroidNotificationScheduler(context).scheduleMonthlyPartyReminder()
    }
}

internal class PartyReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidNotificationScheduler(context).scheduleMonthlyPartyReminder()
    }
}

internal class AndroidNotificationScheduler(
    private val context: Context,
) : NotificationScheduler {
    override fun requestPermissionAndSchedulePartyReminder(
        onPermissionPromptStatusChanged: (NotificationPermissionPromptStatus) -> Unit,
    ) {
        onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.PENDING_CHECK)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            scheduleMonthlyPartyReminder()
            onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
            return
        }

        val preferences = context.getSharedPreferences(PARTY_REMINDER_PREFERENCES, Context.MODE_PRIVATE)
        val alreadyRequested = preferences.getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)
        val activity = context as? MainActivity
        if (alreadyRequested || activity == null ||
            androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
            return
        }

        preferences.edit().putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply()
        onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.PROMPT_REQUIRED_OR_IN_PROGRESS)
        activity.requestNotificationPermission { granted ->
            if (granted) scheduleMonthlyPartyReminder()
            onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
        }
    }

    fun scheduleMonthlyPartyReminder() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val reminderIntent = Intent(context, PartyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            PARTY_REMINDER_REQUEST_CODE,
            reminderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextPartyReminderTime(Calendar.getInstance()).timeInMillis,
            pendingIntent,
        )
    }
}

private fun showPartyReminder(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val notificationManager = context.getSystemService(NotificationManager::class.java)
    notificationManager.createNotificationChannel(
        NotificationChannel(
            PARTY_REMINDER_CHANNEL_ID,
            context.getString(R.string.party_reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ),
    )
    val contentIntent = PendingIntent.getActivity(
        context,
        PARTY_REMINDER_REQUEST_CODE,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    val notification = android.app.Notification.Builder(context, PARTY_REMINDER_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(context.getString(R.string.party_reminder_title))
        .setContentText(context.getString(R.string.party_reminder_body))
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    notificationManager.notify(PARTY_REMINDER_NOTIFICATION_ID, notification)
}

/** Next reminder instant, computed from the shared schedule rule. */
internal fun nextPartyReminderTime(now: Calendar): Calendar {
    val date = upcomingPartyReminders(
        year = now.get(Calendar.YEAR),
        month = now.get(Calendar.MONTH) + 1,
        day = now.get(Calendar.DAY_OF_MONTH),
        hour = now.get(Calendar.HOUR_OF_DAY),
        count = 1,
    ).single()
    return Calendar.getInstance().apply {
        clear()
        set(date.year, date.month - 1, date.day, PARTY_REMINDER_HOUR, 0, 0)
    }
}

actual fun platformNotificationScheduler(): NotificationScheduler {
    val activity = MainActivity.currentActivity
    return if (activity == null) NoOpNotificationScheduler else AndroidNotificationScheduler(activity)
}

private object NoOpNotificationScheduler : NotificationScheduler {
    override fun requestPermissionAndSchedulePartyReminder(
        onPermissionPromptStatusChanged: (NotificationPermissionPromptStatus) -> Unit,
    ) {
        onPermissionPromptStatusChanged(NotificationPermissionPromptStatus.NO_PROMPT_REQUIRED)
    }
}
