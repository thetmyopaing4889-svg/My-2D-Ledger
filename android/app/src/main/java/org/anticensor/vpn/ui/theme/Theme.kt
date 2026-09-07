package org.anticensor.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF02131A),
    primaryContainer = Color(0xFF083344),
    onPrimaryContainer = NeonCyanBright,

    secondary = NeonEmerald,
    onSecondary = Color(0xFF012014),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = NeonEmeraldBright,

    tertiary = NeonViolet,
    onTertiary = Color.White,

    background = BackgroundDark,
    onBackground = TextPrimary,

    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,

    outline = BorderAccent,
    outlineVariant = BorderSubtle,

    error = NeonRose,
    onError = Color.White
)

@Composable
fun AegisVPNTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
