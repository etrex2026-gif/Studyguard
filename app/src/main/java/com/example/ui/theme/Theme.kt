package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudyGuardDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = DeepNavy,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = TextPrimary,
    secondary = TechBlue,
    onSecondary = TextPrimary,
    secondaryContainer = NavyCard,
    onSecondaryContainer = TechBlueLight,
    tertiary = AmberWarning,
    onTertiary = DeepNavy,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondary,
    outline = NavyCardBorder,
    error = CoralRed,
    onError = TextPrimary
)

private val StudyGuardLightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = TextPrimary,
    primaryContainer = EmeraldLight,
    onPrimaryContainer = DeepNavy,
    secondary = TechBlue,
    onSecondary = TextPrimary,
    secondaryContainer = LightCard,
    onSecondaryContainer = TechBlueDark,
    tertiary = AmberWarning,
    onTertiary = DeepNavy,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = CoralRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek dark navy theme as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudyGuardDarkColorScheme else StudyGuardLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                val insetsController = WindowCompat.getInsetsController(it, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
