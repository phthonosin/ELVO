package com.elvo.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/** Alarm + notification plumbing for the to-do list. */
object ReminderScheduler {

    const val ACTION_REMIND = "com.elvo.app.action.REMIND"
    const val ACTION_TASK_REMIND = "com.elvo.app.action.TASK_REMIND"
    const val ACTION_CHECK_IN = "com.elvo.app.action.CHECK_IN"
    const val ACTION_WEEKLY_PLAN = "com.elvo.app.action.WEEKLY_PLAN"

    const val CHANNEL_ID = "elvo_reminders"

    const val EXTRA_DAY = "day"
    const val EXTRA_HOUR = "hour"
    const val EXTRA_MINUTE = "minute"
    const val EXTRA_LABEL = "label"
    const val EXTRA_INDEX = "index"
    const val EXTRA_TIMING_HOUR = "timing_hour"
    const val EXTRA_TIMING_MINUTE = "timing_minute"
    const val EXTRA_DURATION = "duration"
    const val EXTRA_MODE = "mode"
    const val EXTRA_OPEN_TODO = "open_todo"

    const val MODE_DAILY = "daily"
    const val MODE_TASK = "task"
    const val MODE_CHECK_IN = "check_in"

    private const val TASK_CODE_BASE = 3_000_000
    private const val CHECK_IN_CODE_BASE = 5_000_000
    private const val WEEKLY_REQUEST_CODE = 4_000_000
    private const val WEEKLY_DAY = 6
    private const val WEEKLY_HOUR = 18
    private const val WEEKLY_MINUTE = 0

    /** A task the user actually gave both a start time and a duration. */
    data class TaskAlert(
        val dayOfWeek: Int,
        val index: Int,
        val label: String,
        val timing: TaskTiming
    ) {
        val startHour: Int get() = requireNotNull(timing.hour) {
            "A task alert needs a user-written start time."
        }
        val durationMinutes: Int get() = requireNotNull(timing.durationMinutes) {
            "A task alert needs a user-written duration."
        }
        val requestCode: Int get() = TASK_CODE_BASE + offset()
        val checkInRequestCode: Int get() = CHECK_IN_CODE_BASE + offset()

        private fun offset(): Int =
            dayOfWeek * 100_000 + index * 10_000 + startHour * 100 + timing.minute
    }

    private fun alarmManager(context: Context) =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun receiverIntent(context: Context, action: String) =
        Intent(context, ReminderReceiver::class.java).apply { this.action = action }

    private fun broadcast(
        context: Context,
        requestCode: Int,
        intent: Intent,
        flags: Int = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    ) = PendingIntent.getBroadcast(context, requestCode, intent, flags)

    private fun setAlarm(context: Context, at: Long, pending: PendingIntent) {
        val manager = alarmManager(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.set(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    private fun Intent.putTaskExtras(alert: TaskAlert) {
        putExtra(EXTRA_DAY, alert.dayOfWeek)
        putExtra(EXTRA_INDEX, alert.index)
        putExtra(EXTRA_LABEL, alert.label)
        putExtra(EXTRA_TIMING_HOUR, alert.startHour)
        putExtra(EXTRA_TIMING_MINUTE, alert.timing.minute)
        putExtra(EXTRA_DURATION, alert.durationMinutes)
    }

    fun remindersFrom(intent: Intent): WeekReminder? {
        val day = intent.getIntExtra(EXTRA_DAY, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, WeekReminder.DAILY_REMINDER_HOUR)
        val minute = intent.getIntExtra(EXTRA_MINUTE, WeekReminder.DAILY_REMINDER_MINUTE)
        if (day !in 1..7 || hour !in 0..23 || minute !in 0..59) return null
        return WeekReminder(day, hour, minute, intent.getStringExtra(EXTRA_LABEL) ?: "Reminder")
    }

    fun taskAlertFrom(intent: Intent): TaskAlert? {
        val day = intent.getIntExtra(EXTRA_DAY, 0)
        val index = intent.getIntExtra(EXTRA_INDEX, 0)
        val hour = intent.getIntExtra(EXTRA_TIMING_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_TIMING_MINUTE, -1)
        val duration = intent.getIntExtra(EXTRA_DURATION, 0)
        val label = intent.getStringExtra(EXTRA_LABEL)?.trim().orEmpty()
        if (day !in 1..7 || index < 0 || hour !in 0..23 || minute !in 0..59 || duration < 1 || label.isEmpty()) {
            return null
        }
        return TaskAlert(day, index, label, TaskTiming(hour, minute, duration))
    }

    fun taskActivityIntent(context: Context, mode: String, alert: TaskAlert): Intent =
        Intent(context, ReminderActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
            putTaskExtras(alert)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    /** Cancel previous alarms and put the new plan in place. */
    fun reschedule(context: Context, previous: List<WeekReminder>, next: List<WeekReminder>) {
        val days = (previous.map { it.dayOfWeek } + next.map { it.dayOfWeek }).toSet()
        days.forEach { cancelEverySlotForDay(context, it) }
        cancelTaskAlerts(context, previous)
        cancelTaskAlerts(context, next)

        next.forEach { scheduleOne(context, it) }
        next.forEachIndexed { dayIndex, reminder ->
            reminder.tasks.forEachIndexed { taskIndex, task ->
                val timing = task.timing
                if (timing?.canPopFullScreen == true) {
                    scheduleTaskAlert(
                        context,
                        TaskAlert(reminder.dayOfWeek, taskIndex, task.label, timing)
                    )
                }
            }
        }
        scheduleWeeklyPlanPrompt(context)
    }

    /** The ordinary top-bar nudge. It never starts a full-screen activity by itself. */
    fun scheduleOne(context: Context, reminder: WeekReminder) {
        val intent = receiverIntent(context, ACTION_REMIND).apply {
            putExtra(EXTRA_DAY, reminder.dayOfWeek)
            putExtra(EXTRA_HOUR, reminder.hour)
            putExtra(EXTRA_MINUTE, reminder.minute)
            putExtra(EXTRA_LABEL, reminder.label)
        }
        val pending = broadcast(context, reminder.requestCode, intent)
        setAlarm(context, nextOccurrence(reminder.dayOfWeek, reminder.hour, reminder.minute), pending)
    }

    fun snooze(context: Context, reminder: WeekReminder, minutes: Int = 10) {
        val intent = receiverIntent(context, ACTION_REMIND).apply {
            putExtra(EXTRA_DAY, reminder.dayOfWeek)
            putExtra(EXTRA_HOUR, reminder.hour)
            putExtra(EXTRA_MINUTE, reminder.minute)
            putExtra(EXTRA_LABEL, reminder.label)
        }
        val pending = broadcast(context, reminder.requestCode, intent)
        setAlarm(context, System.currentTimeMillis() + minutes * 60_000L, pending)
    }

    private fun scheduleTaskAlert(context: Context, alert: TaskAlert) {
        val intent = receiverIntent(context, ACTION_TASK_REMIND).apply { putTaskExtras(alert) }
        val pending = broadcast(context, alert.requestCode, intent)
        setAlarm(context, nextOccurrence(alert.dayOfWeek, alert.startHour, alert.timing.minute), pending)
    }

    fun snoozeTask(context: Context, alert: TaskAlert, minutes: Int = 10) {
        val intent = receiverIntent(context, ACTION_TASK_REMIND).apply { putTaskExtras(alert) }
        val pending = broadcast(context, alert.requestCode, intent)
        setAlarm(context, System.currentTimeMillis() + minutes * 60_000L, pending)
    }

    /** Started only after "On it" on a task that has a user-written duration. */
    fun scheduleCheckIn(context: Context, alert: TaskAlert, minutes: Int) {
        val intent = receiverIntent(context, ACTION_CHECK_IN).apply { putTaskExtras(alert) }
        val pending = broadcast(context, alert.checkInRequestCode, intent)
        setAlarm(context, System.currentTimeMillis() + minutes * 60_000L, pending)
    }

    fun weeklyRequestCode(): Int = WEEKLY_REQUEST_CODE

    /** Asks for next week's plan every Saturday at 6 PM. */
    fun scheduleWeeklyPlanPrompt(context: Context) {
        val intent = receiverIntent(context, ACTION_WEEKLY_PLAN)
        val pending = broadcast(context, WEEKLY_REQUEST_CODE, intent)
        setAlarm(context, nextWeeklyOccurrence(), pending)
    }

    private fun cancelTaskAlerts(context: Context, plan: List<WeekReminder>) {
        val manager = alarmManager(context)
        plan.forEachIndexed { dayIndex, reminder ->
            reminder.tasks.forEachIndexed { taskIndex, task ->
                val timing = task.timing
                if (timing?.canPopFullScreen != true) return@forEachIndexed
                val alert = TaskAlert(reminder.dayOfWeek, taskIndex, task.label, timing)
                listOf(alert.requestCode to ACTION_TASK_REMIND, alert.checkInRequestCode to ACTION_CHECK_IN)
                    .forEach { (code, action) ->
                        broadcast(
                            context,
                            code,
                            receiverIntent(context, action),
                            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                        )?.let { manager.cancel(it) }
                    }
            }
        }
    }

    private fun cancelEverySlotForDay(context: Context, day: Int) {
        val manager = alarmManager(context)
        val intent = receiverIntent(context, ACTION_REMIND)
        for (hour in 0..23) {
            for (minute in 0..59) {
                val requestCode = day * 10000 + hour * 100 + minute
                broadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )?.let { manager.cancel(it) }
            }
        }
    }

    private fun nextOccurrence(day: Int, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var date = now.toLocalDate()
        repeat(8) {
            val candidate = date.atTime(hour, minute)
            if (candidate.dayOfWeek.value == day && candidate.isAfter(now)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            date = date.plusDays(1)
        }
        return date.atTime(hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun nextWeeklyOccurrence(): Long {
        val now = LocalDateTime.now()
        var date = now.toLocalDate()
        repeat(8) {
            val candidate = date.atTime(WEEKLY_HOUR, WEEKLY_MINUTE)
            if (candidate.dayOfWeek.value == WEEKLY_DAY && candidate.isAfter(now)) {
                return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            date = date.plusDays(1)
        }
        return date.atTime(WEEKLY_HOUR, WEEKLY_MINUTE)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
