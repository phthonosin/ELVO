package com.elvo.app.ui.setup

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
import com.elvo.app.ui.components.DirectivePromptField
import com.elvo.app.ui.components.DirectiveSuggestions
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.SurfaceVariant
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

@Composable
fun BlockPromptScreen(
    initialPrompt: String,
    onBack: () -> Unit = {},
    onContinue: (String) -> Unit
) {
    var promptText by remember { mutableStateOf(initialPrompt) }

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
                text = "ELVO's directive",
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Describe it in your own words.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            DirectivePromptField(
                value = promptText,
                onValueChange = { promptText = it },
                height = 130.dp,
                counterFontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            DirectiveSuggestions(
                onSelect = { promptText = it },
                showHint = true,
                itemVerticalPadding = 15.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onContinue(promptText) },
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
                    Text(text = "Continue", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
