package com.elvo.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.BadgeBgPink
import com.elvo.app.ui.theme.BorderDark
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.SurfaceCard
import com.elvo.app.ui.theme.SurfaceDark
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary
import com.elvo.app.ui.theme.TextSecondary

/** Setup uses roomier cards; Settings uses the denser variant. */
enum class BlockModeCardStyle { Setup, Settings }

@Composable
fun BlockOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    style: BlockModeCardStyle = BlockModeCardStyle.Setup
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentPink else BorderDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioDot(isSelected = isSelected)
            Spacer(modifier = Modifier.width(if (style == BlockModeCardStyle.Setup) 16.dp else 12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = if (style == BlockModeCardStyle.Setup) 17.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (style == BlockModeCardStyle.Settings) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(text = subtitle, fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun BlockModeVideoCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    style: BlockModeCardStyle = BlockModeCardStyle.Setup
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) BadgeBgPink else SurfaceDark,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) AccentPink else BorderDark
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (style == BlockModeCardStyle.Setup) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioDot(isSelected = isSelected)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = "Play a video", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                            Text(text = "Plays a video, then closes", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = AccentPink,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard, RoundedCornerShape(9.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Default video (Rickroll)", fontSize = 13.5.sp, color = TextPrimary)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioDot(isSelected = isSelected)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Play a video", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "Plays a short video, then closes the app", fontSize = 13.sp, color = TextSecondary)
                    }
                }

                if (isSelected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = SurfaceCard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Video", fontSize = 13.5.sp, color = TextPrimary)
                            Text(text = "Default (Rickroll)", fontSize = 13.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioDot(isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .background(PureBlack, CircleShape)
            .border(1.2.dp, if (isSelected) AccentPink else TextMuted, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(modifier = Modifier.size(10.dp).background(AccentPink, CircleShape))
        }
    }
}
