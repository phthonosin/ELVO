package com.elvo.app.ui.setup

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elvo.app.R
import com.elvo.app.ui.components.GlowBackdrop
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.ButtonFill
import com.elvo.app.ui.theme.PureBlack
import com.elvo.app.ui.theme.TextPrimary
import kotlinx.coroutines.delay

/**
 * A short brand beat between the greeting and setup. The directive settles in,
 * "ELVO" carries a slow rose shimmer, and the whole scene fades back to black
 * before the next step begins.
 */
@Composable
fun DirectiveIntroScreen(onFinished: () -> Unit) {
    val exitAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(2450)
        exitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = exitAlpha.value }
        ) {
            GlowBackdrop()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedDirective()
            }
        }
    }
}

@Composable
private fun AnimatedDirective() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1600, easing = FastOutSlowInEasing)
        )
    }

    val breath by rememberInfiniteTransition(label = "directiveBreath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "directiveBreathValue"
    )

    val shimmer by rememberInfiniteTransition(label = "directiveShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "directiveShimmerValue"
    )

    val elvoBrush = Brush.linearGradient(
        colors = listOf(AccentPink, ButtonFill, AccentPink),
        start = Offset(shimmer * 1100f - 550f, 0f),
        end = Offset(shimmer * 1100f - 290f, 0f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            translationY = -3.dp.toPx() * breath
        }
    ) {
        Image(
            painter = painterResource(R.drawable.ic_elvo_mark),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    val markProgress = (reveal.value / 0.35f).coerceIn(0f, 1f)
                    alpha = markProgress
                    val scale = 0.82f + 0.18f * markProgress + 0.015f * breath
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - markProgress) * 14.dp.toPx()
                }
        )

        Spacer(modifier = Modifier.height(26.dp))

        DirectivePhrase(reveal = reveal.value, elvoBrush = elvoBrush)

        Spacer(modifier = Modifier.height(26.dp))

        val lineProgress = ((reveal.value - 0.62f) / 0.38f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .width(136.dp * lineProgress)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            AccentPink.copy(alpha = 0.75f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

private val PHRASE_LINES = listOf(
    listOf("Clean", "your", "eyes"),
    listOf("and", "brain", "with"),
    listOf("ELVO.")
)

@Composable
private fun DirectivePhrase(reveal: Float, elvoBrush: Brush) {
    val baseStyle = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        letterSpacing = (-0.8).sp,
        lineHeight = 42.sp
    )

    var wordIndex = 0
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PHRASE_LINES.forEach { line ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                line.forEach { word ->
                    val index = wordIndex++
                    val start = 0.08f + index * 0.085f
                    val progress = ((reveal - start) / 0.42f).coerceIn(0f, 1f)
                    val eased = progress * progress * (3f - 2f * progress)
                    val wordModifier = Modifier.graphicsLayer {
                        alpha = eased
                        translationY = (1f - eased) * 26.dp.toPx()
                        val scale = 0.9f + 0.1f * eased
                        scaleX = scale
                        scaleY = scale
                    }

                    if (word == "ELVO.") {
                        Row(modifier = wordModifier) {
                            Text(
                                text = "ELVO",
                                style = baseStyle.copy(brush = elvoBrush)
                            )
                            Text(
                                text = ".",
                                style = baseStyle.copy(color = TextPrimary)
                            )
                        }
                    } else {
                        Text(text = word, style = baseStyle, modifier = wordModifier)
                    }
                }
            }
        }
    }
}
