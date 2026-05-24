package com.example.mycutieschedule.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shapes with intentionally large corner radii for a soft, cute aesthetic.
 * Compared to Material 3 defaults, every level is bumped up by ~4–8 dp.
 *
 * Usage: MaterialTheme.shapes.medium / .large / etc.
 */
val CutieShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),   // chips, text fields, snack bars
    small      = RoundedCornerShape(12.dp),  // small cards, buttons
    medium     = RoundedCornerShape(16.dp),  // event cards, day cells, dialogs
    large      = RoundedCornerShape(24.dp),  // bottom sheets, large cards
    extraLarge = RoundedCornerShape(32.dp),  // FAB, full-screen modal bottom sheets
)
