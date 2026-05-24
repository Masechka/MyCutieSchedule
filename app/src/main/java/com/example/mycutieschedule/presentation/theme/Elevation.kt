package com.example.mycutieschedule.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Elevation tokens for MyCutieSchedule.
 *
 * Material 3 uses tonal elevation (color overlay) rather than drop shadows on dark theme.
 * These tokens represent the semantic levels used across the app.
 *
 * Usage:
 *   Card(elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.card))
 *   Surface(shadowElevation = MaterialTheme.elevation.bottomBar)
 *
 * Levels:
 *   none       0dp  — flat surfaces, backgrounds
 *   extraSmall 1dp  — subtle separation (e.g. calendar day cells)
 *   small      2dp  — event cards in week view
 *   card       4dp  — standard cards, dialogs
 *   bottomBar  8dp  — bottom navigation bar
 *   fab        6dp  — floating action button (Material 3 spec)
 *   modal     16dp  — bottom sheets, full-screen dialogs
 */
@Immutable
data class Elevation(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 1.dp,
    val small: Dp = 2.dp,
    val card: Dp = 4.dp,
    val fab: Dp = 6.dp,
    val bottomBar: Dp = 8.dp,
    val modal: Dp = 16.dp,
)

internal val LocalElevation = staticCompositionLocalOf { Elevation() }

/** Access elevation tokens from any composable: `MaterialTheme.elevation.card` */
val MaterialTheme.elevation: Elevation
    @Composable
    @ReadOnlyComposable
    get() = LocalElevation.current
