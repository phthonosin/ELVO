package com.elvo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.scheduler.ToDoTask
import com.elvo.app.scheduler.WeekReminder
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.BorderDark
import com.elvo.app.ui.theme.SurfaceDark
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary

/** Tappable task wording shared by the today view and the week-plan cards. */
@Composable
fun DayTaskRows(
    tasks: List<ToDoTask>,
    dayOfWeek: Int,
    onToggleTask: (Int, Int) -> Unit,
    color: Color = TextPrimary,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp,
    horizontalPadding: Dp = 0.dp,
    bottomPadding: Dp = 10.dp
) {
    tasks.forEachIndexed { index, task ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleTask(dayOfWeek, index) }
                .padding(horizontal = horizontalPadding, vertical = 0.dp)
                .padding(bottom = bottomPadding),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(6.dp)
                    .background(
                        if (task.done) TextMuted else AccentPink,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                AnimatedTaskText(
                    text = task.label,
                    done = task.done,
                    color = color,
                    fontSize = fontSize,
                    lineHeight = lineHeight
                )
                task.timing?.displayLabel()?.let { timing ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = timing, fontSize = 11.5.sp, color = AccentPink)
                }
            }
        }
    }
}

/** One card per day, so the full week reads as a set of separate days. */
@Composable
fun WeekPlanCards(
    plan: List<WeekReminder>,
    today: Int,
    onToggleTask: (Int, Int) -> Unit
) {
    val byDay = plan.associateBy { it.dayOfWeek }

    WeekReminder.DAYS.forEachIndexed { index, dayName ->
        val day = byDay[index + 1]
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(
                1.dp,
                if (day?.dayOfWeek == today) AccentPink.copy(alpha = 0.45f) else BorderDark
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dayName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentPink
                    )
                    if (day?.dayOfWeek == today) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "today", fontSize = 11.5.sp, color = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                val entries = day?.tasks.orEmpty()
                if (entries.isEmpty()) {
                    Text(text = "Nothing planned", fontSize = 13.5.sp, color = TextMuted)
                } else {
                    DayTaskRows(
                        tasks = entries,
                        dayOfWeek = index + 1,
                        onToggleTask = onToggleTask,
                        bottomPadding = 8.dp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}
