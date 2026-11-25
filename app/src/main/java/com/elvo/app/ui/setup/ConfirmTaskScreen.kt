package com.elvo.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.filter.GovernancePlan
import com.elvo.app.filter.PromptRuleEngine
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.*

@Composable
fun ConfirmTaskScreen(
    activePrompt: String = "",
    onBack: () -> Unit = {},
    onConfirm: () -> Unit,
    onEditPrompt: () -> Unit
) {
    var plan by remember { mutableStateOf<GovernancePlan?>(null) }
    var isProcessing by remember { mutableStateOf(true) }

    LaunchedEffect(activePrompt) {
        isProcessing = true
        plan = PromptRuleEngine.generatePlan(activePrompt)
        isProcessing = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
            .padding(24.dp)
    ) {
        QuietGlow(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-100).dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Here's what ELVO will keep away",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Based on your prompt.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            val activePlan = plan
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (isProcessing || activePlan == null) {
                        CircularProgressIndicator(
                            color = AccentPink,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterHorizontally)
                        )
                    } else {
                        activePlan.bulletPoints.forEachIndexed { index, bullet ->
                            BulletRow(title = bullet.bulletText, subtitle = bullet.detailText)
                            if (index < activePlan.bulletPoints.size - 1) {
                                HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 14.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "You can change or remove any of this later in Settings.",
                fontSize = 12.5.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    contentColor = PureBlack
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Looks right", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onEditPrompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(text = "Edit prompt", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BulletRow(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(24.dp)
                .background(SurfaceHeader, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AccentPink, modifier = Modifier.size(13.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary, lineHeight = 22.sp)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = subtitle, fontSize = 13.5.sp, color = TextMuted, lineHeight = 19.sp)
        }
    }
}
