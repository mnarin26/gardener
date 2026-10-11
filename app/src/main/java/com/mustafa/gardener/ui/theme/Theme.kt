package com.mustafa.gardener.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MossDark,
    secondary = SoilDark,
    tertiary = SkySlateDark,
    background = Color(0xFF132018),
    surface = Color(0xFF1B2A20),
    onPrimary = Color(0xFF102015),
    onBackground = Color(0xFFE8F0EA),
    onSurface = Color(0xFFE8F0EA)
)

private val LightColorScheme = lightColorScheme(
    primary = Moss,
    secondary = Soil,
    tertiary = SkySlate,
    background = Color(0xFFF3F6F2),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1B2A20),
    onSurface = Color(0xFF1B2A20)
)

@Composable
fun GardenerTheme(
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
