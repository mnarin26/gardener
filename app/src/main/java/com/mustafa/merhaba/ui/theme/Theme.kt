package com.mustafa.merhaba.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SeaMistDark,
    secondary = SandDark,
    tertiary = LeafDark,
    background = Color(0xFF122028),
    surface = Color(0xFF1A2C35),
    onPrimary = Color(0xFF0D1F24),
    onBackground = Color(0xFFE8F1F8),
    onSurface = Color(0xFFE8F1F8)
)

private val LightColorScheme = lightColorScheme(
    primary = SeaMist,
    secondary = Sand,
    tertiary = Leaf,
    background = Color(0xFFF7F3EA),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B3A4B),
    onSurface = Color(0xFF1B3A4B)
)

@Composable
fun MerhabaTheme(
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
