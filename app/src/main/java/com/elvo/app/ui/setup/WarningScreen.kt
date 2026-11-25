package com.elvo.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.components.TermsContent
import com.elvo.app.ui.theme.*

@Composable
fun WarningScreen(
    onContinue: () -> Unit,
    onNotForMe: () -> Unit
) {
    var acknowledged by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    if (showTerms) {
        AlertDialog(
            onDismissRequest = { showTerms = false },
            title = {
                Text(
                    text = "Terms and conditions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    TermsContent()
                }
            },
            confirmButton = {
                TextButton(onClick = { showTerms = false }) {
                    Text(text = "Close", color = AccentPink, fontSize = 14.sp)
                }
            },
            containerColor = SurfaceDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
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
        QuietGlow(
            phaseOffset = 0.42f,
            strength = 1.35f,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-140).dp, y = 130.dp)
        )

        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Before you continue",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Three things to know before ELVO starts guarding your sight.",
                fontSize = 14.5.sp,
                color = TextSecondary,
                lineHeight = 21.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            WarningPoint(
                title = "Your rules stay on your phone.",
                body = "No account, and no browsing data leaves your phone."
            )
            Spacer(modifier = Modifier.height(20.dp))
            WarningPoint(
                title = "It watches app and browser activity.",
                body = "Only to keep your eyes and mind clean."
            )
            Spacer(modifier = Modifier.height(20.dp))
            WarningPoint(
                title = "Hardcore mode makes uninstalling hard.",
                body = "If that is not for you, stop now and remove the app."
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { acknowledged = !acknowledged },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(if (acknowledged) AccentPink else PureBlack, RoundedCornerShape(5.dp))
                        .border(1.dp, if (acknowledged) AccentPink else TextMuted, RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (acknowledged) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PureBlack,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "I understand how ELVO works",
                    fontSize = 14.5.sp,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinue,
                enabled = acknowledged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    contentColor = PureBlack,
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor = TextMuted
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "I understand, continue", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onNotForMe,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Not for me", fontSize = 14.sp, color = TextSecondary)
            }

            TextButton(
                onClick = { showTerms = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = "Terms and conditions", fontSize = 12.5.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WarningPoint(title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(7.dp)
                .background(AccentPink, CircleShape)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = body, fontSize = 14.sp, color = TextSecondary, lineHeight = 20.sp)
        }
    }
}
