package com.example.mycutieschedule.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mycutieschedule.presentation.theme.spacing

/**
 * Unified empty-state placeholder.
 *
 * Wherever a list ends up empty (no events on a day, no imported schedules,
 * no search results, …) we show this instead of a blank rectangle: a soft
 * round icon badge, a clear headline, an explanatory subtitle, and — when
 * appropriate — a primary action.
 *
 * Animates in with a tiny scale + fade on first appearance so the state
 * change reads as intentional rather than abrupt.
 *
 * Material 3 design tokens only — automatically adapts to every theme
 * (pink / leopard / pink-leopard, light & dark).
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter   = fadeIn(tween(250)) +
            scaleIn(initialScale = 0.92f, animationSpec = tween(250)),
        exit    = fadeOut(),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.spacing.large,
                    vertical   = MaterialTheme.spacing.huge,
                ),
        ) {
            // Soft round icon badge — uses primaryContainer for a brand tint
            // that's subtle enough to feel ambient rather than promotional.
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))

            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center,
            )

            if (subtitle != null) {
                Spacer(Modifier.height(MaterialTheme.spacing.extraSmall))
                Text(
                    text      = subtitle,
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                TextButton(onClick = onActionClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * Empty state that slides up — useful inside a scrolling list when the
 * data set becomes empty after content was previously shown (e.g. filter
 * applied), so the transition feels connected rather than a hard swap.
 */
@Composable
fun EmptyStateSlideUp(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(
            initialOffsetY = { it / 6 },
            animationSpec  = tween(300),
        ) + fadeIn(tween(300)),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        EmptyState(icon = icon, title = title, subtitle = subtitle)
    }
}
