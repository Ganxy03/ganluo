package com.ganluo.jiyi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val JiyiColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryDeep,
    secondary = PrimaryDeep,
    onSecondary = White,
    background = Bg,
    onBackground = Text,
    surface = Card,
    onSurface = Text,
    surfaceVariant = PrimarySoft,
    onSurfaceVariant = TextSub,
    outline = Line,
    error = PrimaryDeep,
    onError = White
)

@Composable
fun JiyiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JiyiColorScheme,
        typography = JiyiTypography,
        content = content
    )
}
