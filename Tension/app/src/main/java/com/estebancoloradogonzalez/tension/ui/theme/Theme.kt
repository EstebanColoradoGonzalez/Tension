package com.estebancoloradogonzalez.tension.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    surfaceContainerLowest = md_theme_light_surfaceContainerLowest,
    surfaceContainerLow = md_theme_light_surfaceContainerLow,
    surfaceContainer = md_theme_light_surfaceContainer,
    surfaceContainerHigh = md_theme_light_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_light_surfaceContainerHighest,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    surfaceContainerLowest = md_theme_dark_surfaceContainerLowest,
    surfaceContainerLow = md_theme_dark_surfaceContainerLow,
    surfaceContainer = md_theme_dark_surfaceContainer,
    surfaceContainerHigh = md_theme_dark_surfaceContainerHigh,
    surfaceContainerHighest = md_theme_dark_surfaceContainerHighest,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
)

@Immutable
data class TensionSemanticColors(
    val progressionPositive: Color = Color.Unspecified,
    val maintenance: Color = Color.Unspecified,
    val regression: Color = Color.Unspecified,
    val exerciseNotStarted: Color = Color.Unspecified,
    val exerciseInExecution: Color = Color.Unspecified,
    val exerciseCompleted: Color = Color.Unspecified,
    val exerciseRowInExecutionBg: Color = Color.Unspecified,
    val exerciseRowCompletedBg: Color = Color.Unspecified,
    val alertCrisis: Color = Color.Unspecified,
    val alertCrisisBg: Color = Color.Unspecified,
    val alertHigh: Color = Color.Unspecified,
    val alertHighBg: Color = Color.Unspecified,
    val alertMedium: Color = Color.Unspecified,
    val alertMediumBg: Color = Color.Unspecified,
    val trendAscending: Color = Color.Unspecified,
    val trendStable: Color = Color.Unspecified,
    val trendDeclining: Color = Color.Unspecified,
    val sessionCompleted: Color = Color.Unspecified,
    val sessionIncomplete: Color = Color.Unspecified,
    val deloadActive: Color = Color.Unspecified,
)

private val LightSemanticColors = TensionSemanticColors(
    progressionPositive = ProgressionPositiveLight,
    maintenance = MaintenanceLight,
    regression = RegressionLight,
    exerciseNotStarted = ExerciseNotStartedLight,
    exerciseInExecution = ExerciseInExecutionLight,
    exerciseCompleted = ExerciseCompletedLight,
    exerciseRowInExecutionBg = ExerciseRowInExecutionBgLight,
    exerciseRowCompletedBg = ExerciseRowCompletedBgLight,
    alertCrisis = AlertCrisisLight,
    alertCrisisBg = AlertCrisisBgLight,
    alertHigh = AlertHighLight,
    alertHighBg = AlertHighBgLight,
    alertMedium = AlertMediumLight,
    alertMediumBg = AlertMediumBgLight,
    trendAscending = TrendAscendingLight,
    trendStable = TrendStableLight,
    trendDeclining = TrendDecliningLight,
    sessionCompleted = SessionCompletedLight,
    sessionIncomplete = SessionIncompleteLight,
    deloadActive = DeloadActiveLight,
)

private val DarkSemanticColors = TensionSemanticColors(
    progressionPositive = ProgressionPositiveDark,
    maintenance = MaintenanceDark,
    regression = RegressionDark,
    exerciseNotStarted = ExerciseNotStartedDark,
    exerciseInExecution = ExerciseInExecutionDark,
    exerciseCompleted = ExerciseCompletedDark,
    exerciseRowInExecutionBg = ExerciseRowInExecutionBgDark,
    exerciseRowCompletedBg = ExerciseRowCompletedBgDark,
    alertCrisis = AlertCrisisDark,
    alertCrisisBg = AlertCrisisBgDark,
    alertHigh = AlertHighDark,
    alertHighBg = AlertHighBgDark,
    alertMedium = AlertMediumDark,
    alertMediumBg = AlertMediumBgDark,
    trendAscending = TrendAscendingDark,
    trendStable = TrendStableDark,
    trendDeclining = TrendDecliningDark,
    sessionCompleted = SessionCompletedDark,
    sessionIncomplete = SessionIncompleteDark,
    deloadActive = DeloadActiveDark,
)

val LocalTensionSemanticColors = staticCompositionLocalOf { TensionSemanticColors() }

@Composable
fun TensionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(
        LocalTensionSemanticColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object TensionThemeExtended {
    val semanticColors: TensionSemanticColors
        @Composable
        get() = LocalTensionSemanticColors.current
}