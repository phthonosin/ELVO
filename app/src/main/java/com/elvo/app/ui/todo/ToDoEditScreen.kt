package com.elvo.app.ui.todo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.scheduler.WeekPlanner
import com.elvo.app.scheduler.WeekReminder
import com.elvo.app.ui.components.DayTaskRows
import com.elvo.app.ui.components.GlowBackdrop
import com.elvo.app.ui.components.WeekPlanCards
import com.elvo.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToDoEditScreen(
    initialDescription: String = "",
    initialPlan: List<WeekReminder> = emptyList(),
    onBack: () -> Unit,
    onSave: (String, List<WeekReminder>) -> Unit,
    onFinished: () -> Unit,
    onToggleTask: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var description by remember { mutableStateOf(initialDescription) }
    var plan by remember { mutableStateOf(initialPlan) }
    var showPlan by remember { mutableStateOf(initialPlan.isNotEmpty()) }
    var showWeekPlan by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val today = LocalDate.now().dayOfWeek.value

    // Toggling a task writes through to DataStore; follow those saved changes.
    LaunchedEffect(initialPlan) { plan = initialPlan }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Reminders work with or without notifications; the screen still opens. */ }

    fun buildPlan() {
        val parsed = WeekPlanner.parse(description)
        error = parsed.isEmpty()
        if (parsed.isNotEmpty()) {
            plan = parsed
            showPlan = true
            showWeekPlan = false
        }
    }

    fun save() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onSave(description, plan)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        GlowBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(bottom = 56.dp)
                .verticalScroll(rememberScrollState())
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Your to-do list",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Write it the way you'd tell a friend. A few lines, commas, or a loose weekend note are enough. Add a clock time and duration only for tasks that truly need one.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it; error = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 190.dp),
                label = { Text("What's coming up", color = TextSecondary) },
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = TextPrimary),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentPink,
                    unfocusedBorderColor = BorderDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = AccentPink,
                    focusedLabelColor = AccentPink,
                    unfocusedLabelColor = TextMuted
                )
            )

            if (error) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "I couldn't find anything to add. Try a few things you want to do.",
                    fontSize = 13.5.sp,
                    color = ErrorTone,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = { buildPlan() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPink, contentColor = PureBlack)
            ) {
                Text("Build my list", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }

            if (showPlan && plan.isNotEmpty()) {
                Spacer(modifier = Modifier.height(26.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showWeekPlan) "Week plan" else "Today's to-dos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    TextButton(onClick = { showWeekPlan = !showWeekPlan }) {
                        Text(
                            text = if (showWeekPlan) "Today" else "Week plan",
                            fontSize = 13.sp,
                            color = AccentPink
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Each day gets a gentle nudge. A task with both a time and a duration gets its own alert and a check-in afterwards.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                if (showWeekPlan) {
                    WeekPlanCards(
                        plan = plan,
                        today = today,
                        onToggleTask = onToggleTask
                    )
                } else {
                    val todayPlan = plan.firstOrNull { it.dayOfWeek == today }
                    val todayName = WeekReminder.DAYS.getOrNull(today - 1) ?: ""
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Today",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentPink
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = todayName, fontSize = 12.5.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            val tasks = todayPlan?.tasks.orEmpty()
                            if (tasks.isEmpty()) {
                                Text(
                                    text = "Nothing on today's list.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    lineHeight = 21.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Open Week plan to see the rest of your days.",
                                    fontSize = 12.5.sp,
                                    color = TextMuted
                                )
                            } else {
                                DayTaskRows(
                                    tasks = tasks,
                                    dayOfWeek = today,
                                    onToggleTask = onToggleTask,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = { save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Brush.horizontalGradient(listOf(AccentPink, ButtonFill)), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = PureBlack)
                ) {
                    Text("Save my to-do list", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onFinished, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now", fontSize = 14.sp, color = TextMuted)
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
