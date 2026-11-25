package com.elvo.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.elvo.app.data.prefs.PreferencesRepository
import com.elvo.app.scheduler.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, WatchdogService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val plan = PreferencesRepository(context).weekPlan.first()
                    ReminderScheduler.reschedule(context, emptyList(), plan)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
