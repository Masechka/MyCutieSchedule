package com.example.mycutieschedule.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the entire app.
 *
 * Navigation Compose 2.8+ supports `@Serializable` routes directly —
 * no more string-based route templates or manual argument parsing.
 * Arguments become constructor parameters with full Kotlin type safety.
 *
 * Structure:
 *   [AuthGraph] nested graph
 *     └─ [Auth]      Welcome / landing screen (start destination)
 *     └─ [SignIn]    Email + password sign-in
 *     └─ [SignUp]    Registration
 *
 *   [MainGraph] nested graph
 *     └─ [MonthCalendar]   Start destination — scrollable month grid
 *     └─ [WeekCalendar]    7-column week view
 *     └─ [DayView]         Hourly day timeline  ← takes dateEpochDay
 *     └─ [EventEditor]     Create / edit an event  ← takes eventId + prefill date
 *     └─ [SharedSchedules] Imported schedules list
 *     └─ [Profile]         User profile & share code
 *     └─ [Settings]        Theme and app preferences
 *
 * Why `sealed class` instead of `sealed interface`?
 *   @Serializable polymorphism requires a sealed class hierarchy.
 *   All subclasses must also be @Serializable.
 */
@Serializable
sealed class Screen {

    // ── Graph roots ───────────────────────────────────────────────────────────
    // These are used as `startDestination` in `navigation<T> { }` blocks.
    // They are never displayed themselves — they're just route keys.

    @Serializable data object AuthGraph : Screen()
    @Serializable data object MainGraph : Screen()

    // ── Auth graph ────────────────────────────────────────────────────────────

    /** Welcome screen: app logo + "Sign In" / "Create account" buttons. */
    @Serializable data object Auth : Screen()

    /** Email + password sign-in form. */
    @Serializable data object SignIn : Screen()

    /** Registration form: email, password, display name. */
    @Serializable data object SignUp : Screen()

    // ── Main graph ────────────────────────────────────────────────────────────

    /**
     * Bottom-nav shell that hosts the Month / Week / More tabs.
     * This is the start destination of [MainGraph] after login.
     */
    @Serializable data object MainShell : Screen()

    /** Month grid calendar — the app's home screen. */
    @Serializable data object MonthCalendar : Screen()

    /** 7-column week view with time slots. */
    @Serializable data object WeekCalendar : Screen()

    /**
     * Hourly day timeline.
     *
     * @param dateEpochDay [java.time.LocalDate.toEpochDay()] — compact and timezone-safe.
     */
    @Serializable data class DayView(val dateEpochDay: Long) : Screen()

    /**
     * Create or edit an event.
     *
     * @param eventId              Existing event's ID. Empty string = create a new event.
     * @param prefillDateEpochMilli Pre-filled start date when creating from a specific day
     *                              (UTC epoch millis). Null = use current time.
     */
    @Serializable data class EventEditor(
        val eventId: String = "",
        val prefillDateEpochMilli: Long? = null,
    ) : Screen()

    /** List of schedules the user has imported or is viewing. */
    @Serializable data object SharedSchedules : Screen()

    /**
     * Read-only event list for a single imported schedule.
     *
     * @param scheduleId [SharedSchedule.id] — equals the share code used to import it.
     */
    @Serializable data class SharedScheduleDetails(val scheduleId: String) : Screen()

    /** User profile: display name, email, share code generator. */
    @Serializable data object Profile : Screen()

    /** App settings: theme (light / dark / system). */
    @Serializable data object Settings : Screen()
}
