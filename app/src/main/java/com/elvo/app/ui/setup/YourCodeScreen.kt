package com.elvo.app.ui.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun YourCodeScreen(
    generatedCode: String,
    onBack: () -> Unit = {},
    onSaved: (Boolean) -> Unit
) {
    var blockUninstallState by remember { mutableStateOf(true) }
    val clipboardManager = LocalClipboardManager.current

    var showFarewell by remember { mutableStateOf(false) }
    val farewellAlpha by animateFloatAsState(
        targetValue = if (showFarewell) 1f else 0f,
        animationSpec = tween(700),
        label = "farewellFade"
    )
    LaunchedEffect(showFarewell) {
        if (showFarewell) {
            delay(1600)
            onSaved(blockUninstallState)
        }
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
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

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hardcore mode",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        letterSpacing = (-0.4).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Prevents the app from being uninstalled without your code.",
                        fontSize = 15.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = blockUninstallState,
                    onCheckedChange = { blockUninstallState = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = AccentPink,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(text = "Your code", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = generatedCode,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary,
                        letterSpacing = 1.2.sp
                    )

                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(generatedCode)) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(SurfaceVariant, RoundedCornerShape(10.dp))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy code", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Save this code. You'll need it to edit the prompt or turn off Hardcore mode.",
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { showFarewell = true },
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
                    Text(text = "Continue", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showFarewell) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureBlack.copy(alpha = farewellAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (blockUninstallState) "now that's what we're talking about" else "suit yourself",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary.copy(alpha = farewellAlpha),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
