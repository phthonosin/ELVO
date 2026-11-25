package com.elvo.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.elvo.app.ui.theme.AccentPink
import com.elvo.app.ui.theme.ButtonFill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** One full drift cycle. Every glow reads the same clock, so tabs never jump. */
private const val GLOW_PERIOD_NANOS = 20_000_000_000L

@Composable
private fun rememberGlowPhase(): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                phase = (frameTimeNanos % GLOW_PERIOD_NANOS).toFloat() / GLOW_PERIOD_NANOS
            }
        }
    }
    return phase
}

/**
 * A soft bubble of rose light. Used on every dark screen to give the near-black
 * surfaces a little depth without breaking the calm.
 *
 * The light drifts on a shared absolute clock, so every instance on every tab
 * breathes at the same phase. [phaseOffset] staggers multiple bubbles on one
 * screen while keeping them stable across navigation. [strength] lifts a
 * bubble's brightness without changing its motion.
 */
@Composable
fun QuietGlow(
    modifier: Modifier = Modifier,
    phaseOffset: Float = 0f,
    strength: Float = 1f
) {
    val phase = rememberGlowPhase()
    val t = (phase + phaseOffset) * 2f * PI.toFloat()
    val driftX = sin(t)
    val driftY = cos(t * 0.8f)
    val breathe = sin(t * 1.1f + 0.7f)
    val pulse = sin(t * 1.35f + 1.2f)
    val alphaScale = strength.coerceIn(0f, 2f)

    Box(
        modifier = modifier
            .size(380.dp)
            .graphicsLayer {
                // The whole bubble floats, so the motion reads even at a glance.
                translationX = driftX * 18.dp.toPx()
                translationY = driftY * 14.dp.toPx()
                val scale = 1f + 0.05f * breathe
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                val width = size.width
                val height = size.height

                // Main rose light: drifts in a slow loop and breathes.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPink.copy(alpha = ((0.13f + 0.03f * breathe) * alphaScale).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(
                            width * (0.55f + 0.04f * driftX),
                            height * (0.45f + 0.035f * driftY)
                        ),
                        radius = width * (0.50f + 0.05f * breathe)
                    )
                )
                // A warmer counter-light that pulses against it.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ButtonFill.copy(alpha = ((0.06f + 0.025f * pulse) * alphaScale).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(
                            width * (0.32f - 0.035f * driftX),
                            height * (0.68f + 0.03f * driftY)
                        ),
                        radius = width * (0.32f + 0.03f * breathe)
                    )
                )
                // A small highlight that wanders across the top edge.
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPink.copy(alpha = ((0.035f + 0.02f * sin(t * 1.6f + 0.4f)) * alphaScale).coerceIn(0f, 1f)),
                            Color.Transparent
                        ),
                        center = Offset(
                            width * (0.68f + 0.03f * driftY),
                            height * (0.30f + 0.03f * driftX)
                        ),
                        radius = width * 0.24f
                    )
                )
            }
    )
}

/** The default backdrop for a dark screen: a few faint bubbles of light. */
@Composable
fun GlowBackdrop(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        QuietGlow(
            phaseOffset = 0f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-140).dp)
        )
        QuietGlow(
            phaseOffset = 0.34f,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-150.dp), y = 150.dp)
                .size(320.dp)
        )
        QuietGlow(
            phaseOffset = 0.67f,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-70).dp, y = (-220).dp)
                .size(420.dp)
        )
    }
}
