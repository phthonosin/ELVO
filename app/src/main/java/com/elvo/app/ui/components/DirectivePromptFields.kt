package com.elvo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.theme.BorderDark
import com.elvo.app.ui.theme.SurfaceDark
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

/** Suggestions shared by the setup prompt and the Settings prompt editor. */
val directiveSuggestions = listOf(
    "No social media after 10 PM",
    "No short videos during focus hours",
    "Pause before opening Reddit"
)

@Composable
fun DirectivePromptField(
    value: String,
    onValueChange: (String) -> Unit,
    height: Dp = 120.dp,
    counterFontSize: TextUnit = 11.5.sp
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= 280) onValueChange(it) },
                placeholder = {
                    Text(
                        "block doomscrolling",
                        color = TextMuted,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, color = TextPrimary)
            )
            Text(
                text = "${value.length} / 280",
                fontSize = counterFontSize,
                color = TextMuted,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun DirectiveSuggestions(
    onSelect: (String) -> Unit,
    showHint: Boolean = false,
    itemVerticalPadding: Dp = 14.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Suggestions", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        if (showHint) {
            Text(text = "Tap to use", fontSize = 12.sp, color = TextMuted)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    directiveSuggestions.forEach { directive ->
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onSelect(directive) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = itemVerticalPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = directive,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
