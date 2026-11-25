package com.elvo.app.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.data.prefs.PreferencesRepository
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val prefsRepo = remember { PreferencesRepository(context) }
    val blockCounts by prefsRepo.blockCounts.collectAsState(initial = emptyMap())

    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val dayCounts = days.map { date -> blockCounts[date.toString()] ?: 0 }
    val todayCount = dayCounts.lastOrNull() ?: 0
    val totalCount = dayCounts.sum()
    val maxCount = dayCounts.maxOrNull() ?: 0
    val average = if (dayCounts.isEmpty()) 0 else (totalCount.toDouble() / dayCounts.size).roundToInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        QuietGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-100).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Analytics",
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLight, thickness = 1.dp)

            Spacer(modifier = Modifier.height(26.dp))

            Text(
                text = "Turned away today",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = todayCount.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPink,
                    letterSpacing = (-1.6).sp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "kept away",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$totalCount kept from your eyes in the last 7 days.",
                fontSize = 13.5.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Last 7 days", fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(text = "Avg $average/day", fontSize = 13.sp, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { index, date ->
                            val count = dayCounts[index]
                            val barHeight = if (maxCount == 0) {
                                4
                            } else {
                                (count * 88 / maxCount).coerceIn(4, 88)
                            }
                            BarItem(
                                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                                heightPx = barHeight,
                                isHighlight = index == days.lastIndex
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Updated as ELVO keeps things away.",
                fontSize = 12.5.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun BarItem(dayLabel: String, heightPx: Int, isHighlight: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(heightPx.dp)
                .background(
                    color = if (isHighlight) AccentPink else SurfaceHeader,
                    shape = RoundedCornerShape(6.dp)
                )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dayLabel,
            fontSize = 11.sp,
            fontWeight = if (isHighlight) FontWeight.Medium else FontWeight.Normal,
            color = if (isHighlight) AccentPink else TextMuted
        )
    }
}
