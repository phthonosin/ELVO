package com.elvo.app.scheduler

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.elvo.app.MainActivity
import com.elvo.app.R

/** Turns scheduler alarms into calm notifications; only task alerts go full screen. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMIND -> onDailyReminder(context, intent)
            ReminderScheduler.ACTION_TASK_REMIND -> onTaskReminder(context, intent, fullScreen = true)
            ReminderScheduler.ACTION_CHECK_IN -> onTaskReminder(context, intent, fullScreen = true, checkingIn = true)
            ReminderScheduler.ACTION_WEEKLY_PLAN -> onWeeklyPlanPrompt(context)
        }
    }

    private fun onDailyReminder(context: Context, intent: Intent) {
        val reminder = ReminderScheduler.remindersFrom(intent) ?: return
        val labels = reminder.taskLabels
        val summary = when {
            labels.isEmpty() -> "A moment for your list"
            labels.size == 1 -> labels.first()
            else -> {
                val first = labels.take(2).joinToString(" · ")
                if (labels.size > 2) "$first · +${labels.size - 2} more" else first
            }
        }

        val open = Intent(context, ReminderActivity::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_MODE, ReminderScheduler.MODE_DAILY)
            putExtra(ReminderScheduler.EXTRA_DAY, reminder.dayOfWeek)
            putExtra(ReminderScheduler.EXTRA_HOUR, reminder.hour)
            putExtra(ReminderScheduler.EXTRA_MINUTE, reminder.minute)
            putExtra(ReminderScheduler.EXTRA_LABEL, reminder.label)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        notify(
            context,
            reminder.requestCode,
            "Today's to-dos",
            "${reminder.dayName()} · $summary",
            open
        )
        ReminderScheduler.scheduleOne(context, reminder)
    }

    private fun onTaskReminder(
        context: Context,
        intent: Intent,
        fullScreen: Boolean,
        checkingIn: Boolean = false
    ) {
        val alert = ReminderScheduler.taskAlertFrom(intent) ?: return
        val mode = if (checkingIn) ReminderScheduler.MODE_CHECK_IN else ReminderScheduler.MODE_TASK
        val open = ReminderScheduler.taskActivityIntent(context, mode, alert)

        val title = if (checkingIn) "Did you finish?" else "Time for ${alert.label}"
        val body = if (checkingIn) {
            "${alert.label} · ${alert.timing.displayLabel().orEmpty()}"
        } else {
            "${alert.timing.displayLabel().orEmpty()} · Tap when you're on it"
        }
        notify(context, if (checkingIn) alert.checkInRequestCode else alert.requestCode, title, body, open)

        if (fullScreen) {
            // ELVO holds overlay permission, which exempts it from background
            // activity-start limits. The notification remains the fallback.
            runCatching { context.startActivity(open) }
        }
    }

    private fun onWeeklyPlanPrompt(context: Context) {
        val open = Intent(context, MainActivity::class.java).apply {
            putExtra(ReminderScheduler.EXTRA_OPEN_TODO, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        notify(
            context,
            ReminderScheduler.weeklyRequestCode(),
            "Plan next week",
            "Add your days. Give a time and duration only to tasks that need one.",
            open
        )
        ReminderScheduler.scheduleWeeklyPlanPrompt(context)
    }

    private fun notify(
        context: Context,
        id: Int,
        title: String,
        text: String,
        openIntent: Intent
    ) {
        val pending = PendingIntent.getActivity(
            context,
            id + 500_000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, notification)
    }
}
