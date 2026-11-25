package com.elvo.app.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentPink,
    secondary = ButtonFill,
    background = PureBlack,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCard,
    onPrimary = PureBlack,
    onSecondary = PureBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark
)

@Composable
fun ElvoTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography
    ) {
        // MaterialTheme seeds LocalTextStyle with typography.bodyLarge, which carries a
        // secondary gray. Strip that color so components (buttons, chips, fields) resolve
        // their text color from LocalContentColor instead of inheriting the gray.
        CompositionLocalProvider(
            LocalTextStyle provides Typography.bodyLarge.copy(color = Color.Unspecified),
            LocalContentColor provides TextPrimary,
            content = content
        )
    }
}
