package com.example.mycutieschedule.domain.model

/**
 * User-selectable theme preference.
 * Persisted in DataStore and observed by the root composable.
 */
enum class ThemeMode {
    /** Follow the device system setting. */
    SYSTEM,

    /** Always use the light (pink) theme. */
    LIGHT,

    /** Always use the dark (deep pink) theme. */
    DARK,
}
