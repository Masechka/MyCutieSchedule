package com.example.mycutieschedule.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Pink Scale ─────────────────────────────────────────────────────────────────
// Main brand palette — used to build Material 3 color roles below.
val Pink50  = Color(0xFFFFF0F5)  // barely blush — backgrounds
val Pink100 = Color(0xFFFFD6E8)  // soft blush
val Pink200 = Color(0xFFFFADD8)  // light pink
val Pink300 = Color(0xFFFF80C0)  // candy pink
val Pink400 = Color(0xFFFF4DA6)  // hot pink (light)
val Pink500 = Color(0xFFE91E8C)  // ★ primary brand color
val Pink600 = Color(0xFFD81B60)  // deep pink
val Pink700 = Color(0xFFC2185B)  // rose
val Pink800 = Color(0xFF880E4F)  // dark rose
val Pink900 = Color(0xFF4A0028)  // midnight rose

// ── Complementary tones ────────────────────────────────────────────────────────
val Blush = Color(0xFFFCE4EC)    // very pale pink — light containers
val Mauve = Color(0xFFE8A0BF)    // dusty pink — outlines, secondary accents
val Nude  = Color(0xFFD4A5B5)    // muted pink — disabled / outline variant

// ── Dark theme surfaces ────────────────────────────────────────────────────────
val DarkSurface0 = Color(0xFF12080E)  // darkest — scrim
val DarkSurface1 = Color(0xFF1E1018)  // background
val DarkSurface2 = Color(0xFF2A1622)  // surface (cards, sheets)
val DarkSurface3 = Color(0xFF3B2030)  // surface variant
val DarkSurface4 = Color(0xFF4D2B3E)  // elevated tonal surface

// ── Content / neutrals ────────────────────────────────────────────────────────
val White     = Color(0xFFFFFFFF)
val NearBlack = Color(0xFF1C0812)  // pink-tinted black for text on light bg

// ── Error ──────────────────────────────────────────────────────────────────────
val ErrorLight          = Color(0xFFB3261E)
val OnErrorLight        = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val ErrorDark           = Color(0xFFFFB4AB)
val OnErrorDark         = Color(0xFF690005)
val ErrorContainerDark  = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFFFDAD6)

// ── Event colors — predefined pink shades ─────────────────────────────────────
// These are the ONLY allowed colors when creating an event.
// Values stay constant across light/dark — events have their own visual identity.
val EventRose      = Color(0xFFFF4DA6)  // hot pink
val EventBlossom   = Color(0xFFFFADD8)  // light blush pink
val EventPeony     = Color(0xFFE91E8C)  // deep peony pink
val EventMauve     = Color(0xFFE8A0BF)  // dusty mauve pink
val EventCoral     = Color(0xFFFF8FA3)  // coral pink
val EventLilac     = Color(0xFFD4A5C9)  // lavender pink
val EventSakura    = Color(0xFFFFD6E8)  // pale sakura
val EventRaspberry = Color(0xFFC2185B)  // dark raspberry

/** Ordered list used by the event color picker UI. */
val AllEventColors = listOf(
    EventPeony,
    EventRose,
    EventRaspberry,
    EventCoral,
    EventBlossom,
    EventMauve,
    EventLilac,
    EventSakura,
)

// ── Imported / shared schedule event colors (gray-pink) ───────────────────────
// Visually distinct so the user always knows which events belong to them.
val ImportedLight  = Color(0xFFD4C4CB)
val ImportedMedium = Color(0xFFB8A8AD)
val ImportedDark   = Color(0xFF7A666D)
