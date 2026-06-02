package com.example.mycutieschedule.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Tiny utility wrapper around [CircularProgressIndicator] that:
 *   - uses the active theme's primary color
 *   - centers itself in its parent
 *   - fades in (no jarring pop)
 *
 * Use for short-lived loading states where a full shimmer skeleton would
 * be overkill (e.g. a transient save spinner). For first-paint of a
 * list/calendar prefer [ShimmerBlock] / [CalendarLoadingSkeleton].
 */
@Composable
fun LoadingPlaceholder(
    modifier: Modifier = Modifier,
    strokeWidthDp: Float = 2.5f,
) {
    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(),
    ) {
        Box(
            modifier         = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                strokeWidth = strokeWidthDp.dp,
                color       = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
