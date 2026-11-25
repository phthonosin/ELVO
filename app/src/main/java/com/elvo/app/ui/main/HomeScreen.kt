package com.elvo.app.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.time.LocalDate
import com.elvo.app.R
import com.elvo.app.scheduler.WeekReminder
import com.elvo.app.service.AccessibilityUtils
import com.elvo.app.ui.components.DayTaskRows
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.components.ShuffledKeypadDialog
import com.elvo.app.ui.components.WeekPlanCards
import com.elvo.app.ui.theme.*

@Composable
fun HomeScreen(
    activePrompt: String,
    masterCode: String,
    blockUninstall: Boolean = true,
    weekPlan: List<WeekReminder> = emptyList(),
    onEditPromptClicked: () -> Unit,
    onOpenToDoList: () -> Unit,
    onToggleTask: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var today by remember { mutableStateOf(LocalDate.now().dayOfWeek.value) }
    var showWeekPlan by remember { mutableStateOf(false) }
    var showKeypadDialog by remember { mutableStateOf(false) }
    var isServiceEnabled by remember {
        mutableStateOf(AccessibilityUtils.isAccessibilityServiceEnabled(context))
    }

    // Android can change the accessibility grant while ELVO is in the background,
    // so refresh whenever the screen comes back to the foreground.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isServiceEnabled = AccessibilityUtils.isAccessibilityServiceEnabled(context)
        today = LocalDate.now().dayOfWeek.value
    }

    if (showKeypadDialog) {
        ShuffledKeypadDialog(
            masterCode = masterCode,
            onDismiss = { showKeypadDialog = false },
            onVerified = {
                showKeypadDialog = false
                onEditPromptClicked()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // A quiet rose wash keeps the top of the screen from feeling flat.
        QuietGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-90).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 64.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Text(
                text = "Home",
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLight, thickness = 1.dp)

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isServiceEnabled) AccentPink else WarmAmber, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isServiceEnabled) "Protection is active" else "Protection is off",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isServiceEnabled) {
                            "ELVO is watching over what reaches your eyes."
                        } else {
                            "Enable Accessibility so ELVO can watch over what's on screen."
                        },
                        fontSize = 14.sp,
                        color = TextSecondary,
                        lineHeight = 21.sp
                    )

                    if (!isServiceEnabled) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            },
                            modifier = Modifier.height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, BorderDark),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPink)
                        ) {
                            Text(
                                text = "Open accessibility settings",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentPink
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showWeekPlan) "Week plan" else "Today's to-do list",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                if (weekPlan.isEmpty()) {
                    Text(
                        text = "Empty",
                        fontSize = 12.5.sp,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text = if (showWeekPlan) "Today" else "Week plan",
                        fontSize = 12.5.sp,
                        color = AccentPink,
                        modifier = Modifier.clickable { showWeekPlan = !showWeekPlan }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (weekPlan.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "No list yet.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onOpenToDoList) {
                            Text(text = "Make a to-do list", fontSize = 13.sp, color = AccentPink)
                        }
                    }
                }
            } else if (showWeekPlan) {
                WeekPlanCards(
                    plan = weekPlan,
                    today = today,
                    onToggleTask = onToggleTask
                )
            } else {
                val todayPlan = weekPlan.firstOrNull { it.dayOfWeek == today }
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
                                onToggleTask = onToggleTask
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            TextButton(
                onClick = onOpenToDoList,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Edit to-do list", fontSize = 13.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ELVO's directive",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (blockUninstall) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (blockUninstall) "Locked" else "Unlocked",
                        fontSize = 12.5.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = activePrompt,
                    fontSize = 15.sp,
                    color = TextPrimary,
                    lineHeight = 23.sp,
                    modifier = Modifier.padding(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (blockUninstall) {
                        showKeypadDialog = true
                    } else {
                        onEditPromptClicked()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Edit prompt", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (blockUninstall) {
                    "Hardcore mode is on. Enter your master code to edit."
                } else {
                    "Hardcore mode is off. The prompt can be edited freely."
                },
                fontSize = 12.5.sp,
                color = TextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun ElvoBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val homeTint by animateColorAsState(
        targetValue = if (selectedTab == 0) AccentPink else TextMuted,
        animationSpec = tween(220),
        label = "homeTint"
    )
    val analyticsTint by animateColorAsState(
        targetValue = if (selectedTab == 1) AccentPink else TextMuted,
        animationSpec = tween(220),
        label = "analyticsTint"
    )
    val settingsTint by animateColorAsState(
        targetValue = if (selectedTab == 2) AccentPink else TextMuted,
        animationSpec = tween(220),
        label = "settingsTint"
    )

    Surface(
        color = PureBlack,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Column {
            HorizontalDivider(color = BorderLight, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(0) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painter = painterResource(R.drawable.ic_tab_home), contentDescription = null, tint = homeTint, modifier = Modifier.size(20.dp))
                    Text(text = "Home", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = homeTint, letterSpacing = 0.2.sp)
                }

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(1) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painter = painterResource(R.drawable.ic_tab_analytics), contentDescription = null, tint = analyticsTint, modifier = Modifier.size(20.dp))
                    Text(text = "Analytics", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = analyticsTint, letterSpacing = 0.2.sp)
                }

                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(2) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(painter = painterResource(R.drawable.ic_tab_settings), contentDescription = null, tint = settingsTint, modifier = Modifier.size(20.dp))
                    Text(text = "Settings", fontSize = 10.5.sp, fontWeight = FontWeight.Medium, color = settingsTint, letterSpacing = 0.2.sp)
                }
            }
        }
    }
}
