package com.example.mycutieschedule.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor

/**
 * Centralized event-color helpers used across all calendar views
 * (Month / Week / Day) and event-card composables.
 *
 * Two distinct palettes:
 *   1. **Owner events** — the regular pink/peony palette from [AllEventColors],
 *      indexed by [EventColor.ordinal]. Vibrant, expressive.
 *   2. **Imported events** — a muted gray-pink palette derived from
 *      [ImportedLight] / [ImportedMedium] / [ImportedDark]. Visually softer,
 *      indexed by [EventColor.ordinal] so the workload visualization (which
 *      relies on consistent color per event) is preserved across views.
 *
 * The imported palette stays cohesive with the app's pink theme while making
 * imported events instantly recognizable.
 */

// ── Imported (gray-pink) palette ──────────────────────────────────────────────
//
// Slightly varied gray-pink shades so multiple imported events on the same day
// don't fuse into a single block. All derived from the existing ImportedLight /
// ImportedMedium / ImportedDark anchors to stay cohesive.
//
// Ordered to match the EventColor enum ordinal — same color slot → same shade.
private val ImportedPalette = listOf(
    Color(0xFFC2A8B2),  // dusty rose-gray (PEONY → muted)
    Color(0xFFCDB3BA),  // soft warm gray-pink (ROSE)
    Color(0xFFAC8E97),  // deeper mauve-gray (RASPBERRY)
    Color(0xFFD0BCC0),  // pale blush-gray (CORAL)
    Color(0xFFDCC8CC),  // very pale gray-pink (BLOSSOM)
    Color(0xFFBFA8AE),  // dusty mauve (MAUVE)
    Color(0xFFC5B0B8),  // lavender-gray (LILAC)
    Color(0xFFE0D2D4),  // softest sakura-gray (SAKURA)
)

// ── Foreground (text) color helpers ──────────────────────────────────────────
//
// A few colors in the regular palette are light enough (BLOSSOM, SAKURA) that
// white text becomes unreadable. The helpers below pick contrast based on the
// background's luminance so we don't have to special-case in every screen.

/**
 * Resolves the background color for an event card.
 * Imported events use the muted gray-pink palette; user-owned events use
 * the regular pink palette.
 */
fun Event.toBackgroundColor(): Color =
    if (isImported) {
        ImportedPalette.getOrElse(color.ordinal) { ImportedMedium }
    } else {
        AllEventColors.getOrElse(color.ordinal) { Pink500 }
    }

/**
 * Resolves the text/icon color that sits on top of [toBackgroundColor].
 * Picks white or near-black based on background luminance so titles stay
 * legible across every color in both palettes, in light and dark themes.
 */
fun Event.toOnBackgroundColor(): Color {
    val bg = toBackgroundColor()
    return if (bg.luminance() > 0.6f) NearBlack else White
}

/**
 * Resolves the secondary/subtitle color — same hue family as the foreground
 * but softened (used for description text, time ranges, source labels).
 */
fun Event.toSecondaryTextColor(): Color =
    toOnBackgroundColor().copy(alpha = 0.7f)

// ── Convenience for EventColor (no Event in scope) ────────────────────────────

/**
 * Returns the regular (non-imported) background color for an [EventColor].
 * Use this only where you don't have an [Event] instance (e.g. the color picker).
 * Prefer [Event.toBackgroundColor] in all event-rendering paths so imported
 * events get the muted palette automatically.
 */
fun EventColor.toComposeColor(): Color =
    AllEventColors.getOrElse(ordinal) { Pink500 }

/**
 * Returns the foreground color paired with [EventColor.toComposeColor].
 */
fun EventColor.onComposeColor(): Color {
    val bg = toComposeColor()
    return if (bg.luminance() > 0.6f) NearBlack else White
}
