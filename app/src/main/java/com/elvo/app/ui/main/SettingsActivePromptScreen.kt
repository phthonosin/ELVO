package com.elvo.app.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.filter.PromptSynthesizer
import com.elvo.app.ui.components.DirectivePromptField
import com.elvo.app.ui.components.DirectiveSuggestions
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.BorderDark
import com.elvo.app.ui.theme.BorderLight
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.SurfaceDark
import com.elvo.app.ui.theme.SurfaceVariant
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

@Composable
fun SettingsActivePromptScreen(
    initialPrompt: String,
    onBack: () -> Unit = {},
    onPromptSaved: (String) -> Unit
) {
    var promptText by remember { mutableStateOf(initialPrompt) }

    val synthesizedRules = remember(promptText) {
        PromptSynthesizer.synthesize(promptText)
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
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "ELVO's directive", fontSize = 21.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLight, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tell ELVO what to keep away from your eyes, in your own words.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            DirectivePromptField(
                value = promptText,
                onValueChange = { promptText = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            DirectiveSuggestions(onSelect = { promptText = it })

            if (synthesizedRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "What this keeps away", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        synthesizedRules.forEachIndexed { index, rule ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = rule.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(text = rule.subtitle, fontSize = 13.5.sp, color = TextSecondary, lineHeight = 20.sp)
                            }
                            if (index < synthesizedRules.lastIndex) {
                                HorizontalDivider(color = BorderDark, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onPromptSaved(promptText) },
                enabled = promptText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    contentColor = PureBlack,
                    disabledContainerColor = SurfaceVariant,
                    disabledContentColor = TextMuted
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Save prompt", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
