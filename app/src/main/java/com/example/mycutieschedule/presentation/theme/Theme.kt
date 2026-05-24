package com.example.mycutieschedule.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Material 3 color schemes ──────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = Pink500,
    onPrimary            = White,
    primaryContainer     = Blush,
    onPrimaryContainer   = Pink800,

    secondary            = Pink300,
    onSecondary          = NearBlack,
    secondaryContainer   = Pink50,
    onSecondaryContainer = Pink700,

    tertiary             = Mauve,
    onTertiary           = NearBlack,
    tertiaryContainer    = Pink100,
    onTertiaryContainer  = Pink800,

    error                = ErrorLight,
    onError              = OnErrorLight,
    errorContainer       = ErrorContainerLight,
    onErrorContainer     = OnErrorContainerLight,

    background           = Pink50,
    onBackground         = NearBlack,

    surface              = White,
    onSurface            = NearBlack,
    surfaceVariant       = Blush,
    onSurfaceVariant     = Pink700,

    outline              = Mauve,
    outlineVariant       = Nude,

    scrim                = NearBlack,
    inverseSurface       = Pink900,
    inverseOnSurface     = Pink100,
    inversePrimary       = Pink200,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Pink300,
    onPrimary            = Pink800,
    primaryContainer     = Pink700,
    onPrimaryContainer   = Pink100,

    secondary            = Pink200,
    onSecondary          = Pink800,
    secondaryContainer   = Pink800,
    onSecondaryContainer = Pink200,

    tertiary             = Mauve,
    onTertiary           = Pink800,
    tertiaryContainer    = Pink600,
    onTertiaryContainer  = Pink100,

    error                = ErrorDark,
    onError              = OnErrorDark,
    errorContainer       = ErrorContainerDark,
    onErrorContainer     = OnErrorContainerDark,

    background           = DarkSurface1,
    onBackground         = Pink50,

    surface              = DarkSurface2,
    onSurface            = Pink50,
    surfaceVariant       = DarkSurface3,
    onSurfaceVariant     = Pink200,

    outline              = Mauve,
    outlineVariant       = DarkSurface4,

    scrim                = DarkSurface0,
    inverseSurface       = Pink100,
    inverseOnSurface     = Pink800,
    inversePrimary       = Pink500,
)

// ── Extended colors ───────────────────────────────────────────────────────────
// Colors that go beyond the Material 3 role system.
// Accessed via MaterialTheme.extended (see extension below).

@Immutable
data class ExtendedColors(
    /** Ordered list of allowed event colors shown in the color picker. */
    val eventColors: List<Color>,
    /** Color for events imported from another user's schedule. */
    val eventImported: Color,
    /**
     * Calendar day-cell intensity levels for the month view heatmap.
     * Index 0 = no events, 1 = low, 2 = medium, 3 = high (5+ events).
     */
    val calendarIntensity: List<Color>,
    /** Circle behind today's date number. */
    val todayIndicator: Color,
    /** Circle behind the currently selected date. */
    val selectedDayBackground: Color,
    /** Text color for day numbers on weekends. */
    val weekendDayColor: Color,
)

private val LightExtendedColors = ExtendedColors(
    eventColors = AllEventColors,
    eventImported = ImportedMedium,
    calendarIntensity = listOf(
        Color(0xFFFFF0F5),  // none   — barely pink
        Color(0xFFFFCCE0),  // low    — soft pink
        Color(0xFFFF80BF),  // medium — candy pink
        Pink500,            // high   — full primary pink
    ),
    todayIndicator = Pink500,
    selectedDayBackground = Blush,
    weekendDayColor = Pink600,
)

private val DarkExtendedColors = ExtendedColors(
    eventColors = AllEventColors,
    eventImported = ImportedDark,
    calendarIntensity = listOf(
        DarkSurface2,       // none
        Color(0xFF4A2030),  // low
        Color(0xFF8B2355),  // medium
        Pink700,            // high
    ),
    todayIndicator = Pink300,
    selectedDayBackground = DarkSurface3,
    weekendDayColor = Pink300,
)

// ── CompositionLocal ──────────────────────────────────────────────────────────

private val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

/** Access extended colors from any composable: `MaterialTheme.extended.todayIndicator` */
val MaterialTheme.extended: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current

// ── Theme entry point ─────────────────────────────────────────────────────────

/**
 * Root theme composable for MyCutieSchedule.
 *
 * [darkTheme] defaults to the system setting but will be overridden in Stage 9
 * by the user's DataStore preference (system / light / dark).
 *
 * Dynamic color is intentionally disabled — the pink palette is our brand identity.
 */
@Composable
fun MyCutieScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = CutieTypography,
            shapes      = CutieShapes,
            content     = content,
        )
    }
}
