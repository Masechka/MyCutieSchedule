package com.example.mycutieschedule.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing tokens for MyCutieSchedule.
 *
 * Every padding, gap, and margin in the app should reference one of these values
 * instead of using arbitrary dp literals. This makes the layout system consistent
 * and easy to adjust globally.
 *
 * Usage:
 *   Modifier.padding(MaterialTheme.spacing.medium)
 *   Spacer(Modifier.height(MaterialTheme.spacing.large))
 *
 * Scale:
 *   extraSmall  4dp  — icon-to-label gap, tight list items
 *   small       8dp  — inner card padding, chip padding
 *   medium     16dp  — standard screen horizontal padding, card content padding
 *   large      24dp  — section gaps, vertical screen padding
 *   extraLarge 32dp  — hero section spacing, between major blocks
 *   huge       48dp  — top/bottom padding on empty states, onboarding
 *   massive    64dp  — large vertical breathing room
 */
@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val huge: Dp = 48.dp,
    val massive: Dp = 64.dp,
)

internal val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Access spacing tokens from any composable: `MaterialTheme.spacing.medium` */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
