package com.example.mycutieschedule.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mycutieschedule.presentation.theme.spacing

/**
 * Shimmer-loading modifier.
 *
 * Draws a horizontally sweeping highlight gradient over the surface to
 * indicate "content is loading here". Uses the active theme's `surfaceVariant`
 * + `primaryContainer` so the shimmer reads as the right tone in every
 * theme (pink, leopard, pink-leopard, light & dark) without per-theme code.
 *
 * Typical usage:
 * ```
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .height(56.dp)
 *         .clip(RoundedCornerShape(12.dp))
 *         .shimmer()
 * )
 * ```
 *
 * The animation is driven by [rememberInfiniteTransition] — Compose pauses
 * it automatically when the screen is offscreen, so no CPU is wasted on
 * unseen shimmers.
 */
@Composable
fun Modifier.shimmer(durationMillis: Int = 1_400): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer-transition")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-translate",
    )

    val baseColor      = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primaryContainer

    return this
        .background(baseColor)
        .drawBehind {
            // drawBehind re-runs on every frame, which is exactly what we need
            // to animate translate without invalidating a cache. The cost is
            // negligible — building a LinearGradient each frame is cheap.
            val width     = size.width
            val bandWidth = width * 2f          // sweep covers >surface for clean entry/exit
            val startX    = -bandWidth + translate * (width + bandWidth)

            val brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    highlightColor.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                start = Offset(startX, 0f),
                end   = Offset(startX + bandWidth, 0f),
            )
            drawRect(brush, size = size)
        }
}

/**
 * Single shimmering rectangle — building block for skeleton screens.
 * Pre-applies rounded corners + theme background so call sites stay short.
 */
@Composable
fun ShimmerBlock(
    modifier: Modifier = Modifier,
    cornerRadiusDp: Int = 12,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadiusDp.dp))
            .shimmer(),
    )
}

/**
 * Calendar-grid skeleton — used while month/week data is loading from Room
 * for the first time. Shows a header row + 4 rows of cells, all shimmering.
 *
 * Replaces a bare `CircularProgressIndicator` for a more polished first-paint.
 */
@Composable
fun CalendarLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        // Title row (e.g. "Апрель 2026")
        ShimmerBlock(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(28.dp),
        )
        Spacer(Modifier.height(MaterialTheme.spacing.small))
        // 4 fake week rows
        repeat(4) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            )
        }
    }
}

/**
 * Card-list skeleton — used for SharedSchedulesScreen, EventList, etc.
 * Three rounded blocks of decreasing emphasis.
 */
@Composable
fun ListLoadingSkeleton(
    itemCount: Int = 3,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        repeat(itemCount) {
            ShimmerBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
    }
}
