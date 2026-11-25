package com.elvo.app.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.components.ShuffledKeypadDialog
import com.elvo.app.ui.theme.*

@Composable
fun SettingsScreen(
    blockUninstall: Boolean,
    masterCode: String,
    onToggleBlockUninstall: (Boolean) -> Unit,
    onNavigateToDoList: () -> Unit,
    onNavigateHowToBlock: () -> Unit,
    onNavigateEditPrompt: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onNavigateTerms: () -> Unit
) {
    var showKeypadDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    if (showKeypadDialog) {
        ShuffledKeypadDialog(
            masterCode = masterCode,
            onDismiss = {
                showKeypadDialog = false
                pendingAction = null
            },
            onVerified = {
                showKeypadDialog = false
                pendingAction?.invoke()
                pendingAction = null
            }
        )
    }

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
                .padding(24.dp)
        ) {
            Text(
                text = "Settings",
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
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Hardcore mode", fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(text = "Turning it off asks for your code first", fontSize = 13.5.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = blockUninstall,
                            onCheckedChange = { targetState ->
                                if (!targetState) {
                                    pendingAction = { onToggleBlockUninstall(false) }
                                    showKeypadDialog = true
                                } else {
                                    onToggleBlockUninstall(true)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = AccentPink,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(color = BorderDark)

                    SettingsActionRow(
                        title = "To Do List",
                        subtitle = "Daily nudges + Saturday 6 PM planning prompt",
                        onClick = onNavigateToDoList
                    )

                    HorizontalDivider(color = BorderDark)

                    SettingsActionRow(
                        title = "Blocking behavior",
                        subtitle = "What happens when something is blocked",
                        onClick = onNavigateHowToBlock
                    )

                    HorizontalDivider(color = BorderDark)

                    SettingsActionRow(
                        title = "ELVO's directive",
                        subtitle = if (blockUninstall) "Edit the rules (code required)" else "Edit the rules",
                        onClick = {
                            if (blockUninstall) {
                                pendingAction = onNavigateEditPrompt
                                showKeypadDialog = true
                            } else {
                                onNavigateEditPrompt()
                            }
                        }
                    )

                    HorizontalDivider(color = BorderDark)

                    AdultFilterStatusRow()
                }
            }

            Spacer(modifier = Modifier.height(26.dp))
            Text(text = "Permissions", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsActionRow(title = "Accessibility", subtitle = "Required to watch over what you see", onClick = onOpenAccessibilitySettings)
                    HorizontalDivider(color = BorderDark)
                    SettingsActionRow(title = "Display over other apps", subtitle = "Required for block warnings", onClick = onOpenOverlaySettings)
                    HorizontalDivider(color = BorderDark)
                    SettingsActionRow(title = "Battery optimization", subtitle = "Keeps ELVO running in the background", onClick = onOpenBatterySettings)
                }
            }

            Spacer(modifier = Modifier.height(26.dp))
            Text(text = "About", fontSize = 13.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsActionRow(
                    title = "Terms and conditions",
                    subtitle = "How ELVO keeps your sight clean",
                    onClick = onNavigateTerms
                )
            }
        }
    }
}

@Composable
private fun AdultFilterStatusRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "Sight & mind guard", fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Browsers stay open; unclean pages are kept from your eyes.",
                fontSize = 13.5.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).background(AccentPink, CircleShape))
            Spacer(modifier = Modifier.width(7.dp))
            Text(text = "On", fontSize = 12.5.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, fontSize = 13.5.sp, color = TextSecondary)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
    }
}
