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
    tertiary = PrimaryContainer,
    onTertiary = PrimaryDeep,
    background = Bg,
    onBackground = Text,
    surface = Surface,
    onSurface = Text,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSub,
    surfaceContainerLowest = Bg,
    surfaceContainerLow = Surface,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = PrimaryContainer,
    surfaceContainerHighest = PrimarySoft,
    outline = Line,
    outlineVariant = PrimaryContainer,
    error = Error,
    onError = White
)

@Composable
fun JiyiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JiyiColorScheme,
        typography = JiyiTypography,
        shapes = JiyiShapes,
        content = content
    )
}
