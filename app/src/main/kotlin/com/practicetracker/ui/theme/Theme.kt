package com.practicetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Burgundy700,
    onPrimary = OnBurgundy,
    primaryContainer = Burgundy100,
    onPrimaryContainer = Burgundy900,
    secondary = Amber700,
    onSecondary = OnAmber,
    secondaryContainer = Amber100,
    onSecondaryContainer = Color(0xFF2A1A00),
    tertiary = Sage500,
    onTertiary = OnSage,
    tertiaryContainer = Sage100,
    onTertiaryContainer = Sage700,
    background = WarmOffWhite,
    onBackground = DarkCharcoal,
    surface = WarmWhite,
    onSurface = DarkCharcoal,
    surfaceVariant = Amber100,
    onSurfaceVariant = Color(0xFF4A3B2E),
    error = ErrorRed,
    onError = OnBurgundy,
    outline = Color(0xFFAA9585)
)

private val DarkColorScheme = darkColorScheme(
    primary = Burgundy200,
    onPrimary = Burgundy900,
    primaryContainer = Burgundy700,
    onPrimaryContainer = Burgundy100,
    secondary = Amber200,
    onSecondary = Color(0xFF2A1A00),
    secondaryContainer = Amber700,
    onSecondaryContainer = Amber100,
    tertiary = Sage200,
    onTertiary = Sage700,
    tertiaryContainer = Sage700,
    onTertiaryContainer = Sage100,
    background = DarkCharcoal,
    onBackground = DarkOnSurface,
    surface = DarkWarm,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = Color(0xFFCFC3B4),
    error = ErrorRedDark,
    onError = Color(0xFF601410),
    outline = Color(0xFF7D6E5F)
)

@Composable
fun PracticeTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
