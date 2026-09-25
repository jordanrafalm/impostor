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
import java.util.Calendar

private const val PARTY_REMINDER_CHANNEL_ID = "party_reminders"
private const val PARTY_REMINDER_NOTIFICATION_ID = 2200
private const val PARTY_REMINDER_REQUEST_CODE = 2200

private const val PARTY_REMINDER_TITLE = "Impreza?"
private const val PARTY_REMINDER_BODY = "Zagraj ze znajomymi w Impostora!"

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
    override fun requestPermissionAndSchedulePartyReminder() {
        scheduleMonthlyPartyReminder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            (context as? MainActivity)?.requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                PARTY_REMINDER_REQUEST_CODE,
            )
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
            "Zaproszenia do gry",
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
        .setContentTitle(PARTY_REMINDER_TITLE)
        .setContentText(PARTY_REMINDER_BODY)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    notificationManager.notify(PARTY_REMINDER_NOTIFICATION_ID, notification)
}

/**
 * The first weekend of each month at 22:00: Friday in odd-numbered months,
 * Saturday in even-numbered months.
 */
internal fun nextPartyReminderTime(now: Calendar): Calendar {
    val candidate = partyReminderForMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
    return if (candidate.after(now)) {
        candidate
    } else {
        partyReminderForMonth(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1)
    }
}

internal fun partyReminderForMonth(year: Int, zeroBasedMonth: Int): Calendar {
    val monthStart = Calendar.getInstance().apply {
        clear()
        set(year, zeroBasedMonth, 1, 22, 0, 0)
    }
    val targetDay = if ((zeroBasedMonth + 1) % 2 == 1) Calendar.FRIDAY else Calendar.SATURDAY
    val daysUntilTarget = (targetDay - monthStart.get(Calendar.DAY_OF_WEEK) + 7) % 7
    return monthStart.apply { add(Calendar.DAY_OF_MONTH, daysUntilTarget) }
}

actual fun platformNotificationScheduler(): NotificationScheduler {
    val activity = MainActivity.currentActivity
    return if (activity == null) NoOpNotificationScheduler else AndroidNotificationScheduler(activity)
}

private object NoOpNotificationScheduler : NotificationScheduler {
    override fun requestPermissionAndSchedulePartyReminder() = Unit
}
