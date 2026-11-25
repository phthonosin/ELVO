package com.elvo.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.TextMuted
import com.elvo.app.ui.theme.TextPrimary

/**
 * Task wording with a slash drawn over it when the task is done.
 * The line sweeps left-to-right and the text softly dims, so a tap reads as
 * a small ceremony rather than an abrupt strikethrough.
 */
@Composable
fun AnimatedTaskText(
    text: String,
    done: Boolean,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit = 20.sp,
    fontWeight: FontWeight? = null,
    slashColor: Color = AccentPink
) {
    val slashProgress by animateFloatAsState(
        targetValue = if (done) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "taskSlashProgress"
    )
    val textColor by animateColorAsState(
        targetValue = if (done) TextMuted else color,
        animationSpec = tween(220),
        label = "taskTextColor"
    )

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = textColor,
        lineHeight = lineHeight,
        modifier = modifier.drawWithContent {
            drawContent()
            if (slashProgress > 0f) {
                val y = size.height / 2f
                val inset = size.width * 0.02f
                val end = inset + (size.width - inset * 2) * slashProgress
                drawLine(
                    color = slashColor.copy(alpha = 0.92f * slashProgress),
                    start = Offset(inset, y),
                    end = Offset(end, y),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    )
}
