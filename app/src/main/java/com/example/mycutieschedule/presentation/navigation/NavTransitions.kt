package com.example.mycutieschedule.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/**
 * Shared animation definitions for screen transitions.
 *
 * Three families:
 *   1. **Horizontal slide** — standard forward/back navigation within a graph.
 *      The exiting screen slides to the left at ¼ speed + fades, giving depth.
 *   2. **Modal (vertical)** — bottom-sheet-style entry for EventEditor and Settings.
 *      Feels lighter than a full slide, appropriate for overlay-style screens.
 *   3. **Fade** — used for auth ↔ main graph switches where slides feel jarring.
 *
 * Usage in NavGraph:
 * ```kotlin
 * composable<Screen.SignIn>(
 *     enterTransition    = NavTransitions.slideFromRight,
 *     exitTransition     = NavTransitions.slideToLeft,
 *     popEnterTransition = NavTransitions.slideFromLeft,
 *     popExitTransition  = NavTransitions.slideToRight,
 * )
 * ```
 */
object NavTransitions {

    private const val DURATION    = 300
    private const val MODAL_DURATION = 380

    // ── Horizontal slide ──────────────────────────────────────────────────────

    /** New screen slides in from the right (push forward). */
    val slideFromRight: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { it },
            animationSpec  = tween(DURATION),
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /** Current screen slides out to the left (push forward). */
    val slideToLeft: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX  = { -it / 4 },   // subtle parallax — doesn't fully leave
            animationSpec  = tween(DURATION),
        ) + fadeOut(animationSpec = tween(DURATION))
    }

    /** Previous screen slides back in from the left (pop back). */
    val slideFromLeft: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec  = tween(DURATION),
        ) + fadeIn(animationSpec = tween(DURATION))
    }

    /** Current screen slides out to the right (pop back). */
    val slideToRight: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX  = { it },
            animationSpec  = tween(DURATION),
        ) + fadeOut(animationSpec = tween(DURATION))
    }

    // ── Modal (vertical) ──────────────────────────────────────────────────────

    /** Screen slides up from the bottom (modal open). */
    val slideUp: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { it },
            animationSpec  = tween(MODAL_DURATION),
        ) + fadeIn(animationSpec = tween(MODAL_DURATION / 2))
    }

    /** Screen slides down out of view (modal close). */
    val slideDown: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY  = { it },
            animationSpec  = tween(MODAL_DURATION),
        ) + fadeOut(animationSpec = tween(MODAL_DURATION / 2))
    }

    // ── Fade ──────────────────────────────────────────────────────────────────

    /** Simple fade-in — used for graph-level transitions (auth ↔ main). */
    val fadeIn: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(DURATION))
    }

    /** Simple fade-out — used for graph-level transitions. */
    val fadeOut: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(DURATION))
    }
}
