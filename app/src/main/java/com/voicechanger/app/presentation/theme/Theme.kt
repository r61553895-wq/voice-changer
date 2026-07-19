package com.voicechanger.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoiceChangerColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = NeonCyan,
    tertiary = NeonPink,
    background = BgDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun VoiceChangerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoiceChangerColorScheme,
        typography = VoiceChangerTypography,
        content = content
    )
}
