package com.example.mycutieschedule.domain.model

/**
 * Predefined color palette for events.
 *
 * Only these values are allowed when creating or editing an event.
 * The actual ARGB colors are defined in the presentation layer (Color.kt)
 * to keep the domain free of UI dependencies.
 *
 * The mapping domain → Compose Color lives in:
 *   presentation/theme/Color.kt → AllEventColors list
 *   (ordered to match this enum's ordinal)
 */
enum class EventColor {
    PEONY,       // deep pink  — #E91E8C
    ROSE,        // hot pink   — #FF4DA6
    RASPBERRY,   // dark pink  — #C2185B
    CORAL,       // coral pink — #FF8FA3
    BLOSSOM,     // light pink — #FFADD8
    MAUVE,       // dusty pink — #E8A0BF
    LILAC,       // lavender   — #D4A5C9
    SAKURA,      // pale pink  — #FFD6E8
}
