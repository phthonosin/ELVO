package com.elvo.app.ui.main

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
import com.elvo.app.domain.model.BlockMode
import com.elvo.app.ui.components.BlockModeCardStyle
import com.elvo.app.ui.components.BlockModeVideoCard
import com.elvo.app.ui.components.BlockOptionCard
import com.elvo.app.ui.components.QuietGlow
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.BorderLight
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

@Composable
fun SettingsBlockingBehaviorScreen(
    initialMode: BlockMode = BlockMode.VIDEO,
    onBack: () -> Unit = {},
    onModeSaved: (BlockMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(initialMode) }

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
                Text(text = "Blocking behavior", fontSize = 21.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = BorderLight, thickness = 1.dp)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Choose what happens when you open something blocked.",
                fontSize = 15.sp,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            BlockOptionCard(
                title = "Normal block",
                subtitle = "Closes the app right away",
                isSelected = selectedMode == BlockMode.NORMAL,
                onClick = { selectedMode = BlockMode.NORMAL },
                style = BlockModeCardStyle.Settings
            )

            Spacer(modifier = Modifier.height(12.dp))

            BlockModeVideoCard(
                isSelected = selectedMode == BlockMode.VIDEO,
                onClick = { selectedMode = BlockMode.VIDEO },
                style = BlockModeCardStyle.Settings
            )

            Spacer(modifier = Modifier.height(12.dp))

            BlockOptionCard(
                title = "Reflection prompt",
                subtitle = "Pauses and asks you to think it through",
                isSelected = selectedMode == BlockMode.REFLECTION,
                onClick = { selectedMode = BlockMode.REFLECTION },
                style = BlockModeCardStyle.Settings
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = { onModeSaved(selectedMode) },
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
                    Text(text = "Save", fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = PureBlack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
