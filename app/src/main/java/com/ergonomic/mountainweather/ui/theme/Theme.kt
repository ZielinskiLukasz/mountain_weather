package com.ergonomic.mountainweather.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Teal80,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    background = BackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Teal40,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    background = BackgroundLight
)

@Composable
fun MountainWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
