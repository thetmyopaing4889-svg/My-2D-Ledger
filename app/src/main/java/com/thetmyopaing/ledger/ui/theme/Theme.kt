package com.thetmyopaing.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF35D6B4),
    onPrimary = Color(0xFF00211A),
    secondary = Color(0xFFFFC66B),
    background = Color(0xFF09111F),
    surface = Color(0xFF101C2C),
    surfaceVariant = Color(0xFF1A2A3D),
    onSurface = Color(0xFFE7F0F4),
    onSurfaceVariant = Color(0xFFB6C7CE),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006B5A),
    onPrimary = Color.White,
    secondary = Color(0xFF8A5200),
    background = Color(0xFFF4F8F8),
    surface = Color.White,
    surfaceVariant = Color(0xFFE1ECEB),
    onSurface = Color(0xFF152027),
    onSurfaceVariant = Color(0xFF53666B),
)

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LedgerTypography,
        content = content,
    )
}