package com.ruru.practice.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D6358),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC0E9D9),
    onPrimaryContainer = Color(0xFF002019),
    secondary = Color(0xFF4D635A),
    secondaryContainer = Color(0xFFD0E8DC),
    background = Color(0xFFF8FBF8),
    surface = Color(0xFFF8FBF8),
    surfaceVariant = Color(0xFFDDE5DF)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA4D0BF),
    onPrimary = Color(0xFF07372C),
    primaryContainer = Color(0xFF265044),
    onPrimaryContainer = Color(0xFFC0E9D9),
    secondary = Color(0xFFB4CCC0),
    secondaryContainer = Color(0xFF354B42),
    background = Color(0xFF101512),
    surface = Color(0xFF101512),
    surfaceVariant = Color(0xFF3E4943)
)

@Composable
fun RuruPracticeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
