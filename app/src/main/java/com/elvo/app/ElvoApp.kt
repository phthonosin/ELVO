package com.elvo.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.elvo.app.scheduler.ReminderScheduler

class ElvoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            ReminderScheduler.CHANNEL_ID,
            "To Do List reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Gentle nudges for your to-do list"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }
}
