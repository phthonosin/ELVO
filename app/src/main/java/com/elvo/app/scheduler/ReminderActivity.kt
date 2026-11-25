package com.elvo.app.scheduler

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.elvo.app.data.prefs.PreferencesRepository
import com.elvo.app.ui.components.AnimatedTaskText
import com.elvo.app.ui.components.GlowBackdrop
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.ElvoTheme
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The calm list / check-in shown only for task alerts the user scheduled. */
class ReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(ReminderScheduler.EXTRA_MODE) ?: ReminderScheduler.MODE_DAILY
        val dailyReminder = ReminderScheduler.remindersFrom(intent)
        val taskAlert = ReminderScheduler.taskAlertFrom(intent)
        val prefsRepo = PreferencesRepository(this)

        setContent {
            ElvoTheme {
                var currentReminder by remember { mutableStateOf(dailyReminder) }

                when (mode) {
                    ReminderScheduler.MODE_TASK -> TaskReminderScreen(
                        alert = taskAlert,
                        onOnIt = {
                            taskAlert?.let { alert ->
                                val duration = alert.durationMinutes
                                if (duration > 0) {
                                    ReminderScheduler.scheduleCheckIn(this@ReminderActivity, alert, duration)
                                    Toast.makeText(
                                        this@ReminderActivity,
                                        "I'll check back in ${durationLabel(duration)}.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            finish()
                        },
                        onSnooze = {
                            taskAlert?.let { ReminderScheduler.snoozeTask(this@ReminderActivity, it) }
                            finish()
                        }
                    )

                    ReminderScheduler.MODE_CHECK_IN -> CheckInScreen(
                        alert = taskAlert,
                        onDone = {
                            taskAlert?.let { alert ->
                                lifecycleScope.launch {
                                    prefsRepo.setWeekTaskDone(alert.dayOfWeek, alert.index, true)
                                }
                            }
                        },
                        onFinished = { finish() },
                        onNotYet = {
                            taskAlert?.let {
                                ReminderScheduler.scheduleCheckIn(this@ReminderActivity, it, 10)
                            }
                            Toast.makeText(
                                this@ReminderActivity,
                                "I'll ask again in 10 minutes.",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    )

                    else -> DailyReminderScreen(
                        reminder = currentReminder,
                        onToggleTask = { day, index ->
                            currentReminder?.let { reminder ->
                                val nextDone = !(reminder.tasks.getOrNull(index)?.done ?: false)
                                currentReminder = reminder.withTaskDone(index, nextDone)
                                lifecycleScope.launch { prefsRepo.setWeekTaskDone(day, index, nextDone) }
                            }
                        },
                        onOnIt = { finish() },
                        onSnooze = {
                            dailyReminder?.let { ReminderScheduler.snooze(this@ReminderActivity, it) }
                            finish()
                        }
                    )
                }
            }
        }
    }
}

private fun durationLabel(minutes: Int): String =
    if (minutes >= 60 && minutes % 60 == 0) "${minutes / 60}h" else "${minutes}m"

@Composable
private fun TaskReminderScreen(
    alert: ReminderScheduler.TaskAlert?,
    onOnIt: () -> Unit,
    onSnooze: () -> Unit
) {
    ReminderFrame(
        kicker = "TIME TO BEGIN",
        title = alert?.label ?: "Your task",
        subtitle = alert?.timing?.displayLabel(),
        primaryLabel = "On it",
        onPrimary = onOnIt,
        secondaryLabel = "Snooze 10 minutes",
        onSecondary = onSnooze
    )
}

@Composable
private fun CheckInScreen(
    alert: ReminderScheduler.TaskAlert?,
    onDone: () -> Unit,
    onFinished: () -> Unit,
    onNotYet: () -> Unit
) {
    var confirmed by remember { mutableStateOf(false) }

    // Let the slash finish sweeping across the wording before leaving the popup.
    LaunchedEffect(confirmed) {
        if (confirmed) {
            delay(700)
            onFinished()
        }
    }

    ReminderFrame(
        kicker = "A QUICK CHECK-IN",
        title = "Did you finish?",
        subtitle = if (confirmed) null else alert?.label,
        subtitleContent = if (confirmed) {
            {
                AnimatedTaskText(
                    text = alert?.label.orEmpty(),
                    done = true,
                    color = TextSecondary,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
            }
        } else null,
        primaryLabel = if (confirmed) "Done" else "Yes, done",
        onPrimary = {
            if (!confirmed) {
                confirmed = true
                onDone()
            }
        },
        secondaryLabel = "Not yet — ask again in 10",
        onSecondary = onNotYet
    )
}

@Composable
private fun DailyReminderScreen(
    reminder: WeekReminder?,
    onToggleTask: (Int, Int) -> Unit,
    onOnIt: () -> Unit,
    onSnooze: () -> Unit
) {
    val tasks = reminder?.tasks.orEmpty()
    val dayOfWeek = reminder?.dayOfWeek ?: 0
    ReminderFrame(
        kicker = "TODAY'S LIST",
        title = "Your to-do list",
        subtitle = reminder?.dayName(),
        primaryLabel = "On it",
        onPrimary = onOnIt,
        secondaryLabel = "Snooze 10 minutes",
        onSecondary = onSnooze,
        content = {
            if (tasks.isEmpty()) {
                Text(
                    text = "Nothing on your list right now.",
                    fontSize = 16.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                tasks.forEachIndexed { index, task ->
                    if (index > 0) HorizontalDivider(color = TextMuted.copy(alpha = 0.18f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTask(dayOfWeek, index) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(7.dp)
                                .background(
                                    if (task.done) TextMuted else AccentPink,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedTaskText(
                                text = task.label,
                                done = task.done,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                lineHeight = 24.sp
                            )
                            task.timing?.displayLabel()?.let { label ->
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = label, fontSize = 12.5.sp, color = AccentPink)
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun ReminderFrame(
    kicker: String,
    title: String,
    subtitle: String? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        GlowBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            Text(
                text = kicker,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AccentPink,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )
            if (subtitleContent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                subtitleContent()
            } else if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = subtitle, fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(28.dp))
            content()

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink, contentColor = PureBlack)
            ) {
                Text(text = primaryLabel, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            TextButton(onClick = onSecondary) {
                Text(text = secondaryLabel, fontSize = 14.sp, color = TextMuted)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
