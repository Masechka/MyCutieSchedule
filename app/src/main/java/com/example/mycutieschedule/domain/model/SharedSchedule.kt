package com.example.mycutieschedule.domain.model

import java.time.LocalDateTime

/**
 * Another user's schedule that the current user has accessed.
 *
 * Two access modes:
 *   1. **View-only** ([isImported] = false) — events displayed in gray-pink on the calendar,
 *      read-only, no local copy.
 *   2. **Imported** ([isImported] = true) — events were copied into the user's own calendar,
 *      visually distinguished but editable.
 *
 * Stored locally in Room (shared_schedule_table) and remotely in Firestore
 * under the recipient user's document.
 *
 * @param id           Local identifier.
 * @param scheduleId   The source schedule's Firestore document ID.
 * @param shareCode    The code that was used to access this schedule.
 * @param ownerName    Display name of the schedule owner (for the UI list).
 * @param events       Snapshot of events from the shared schedule.
 * @param isImported   Whether the user chose to import events into their own calendar.
 * @param accessedAt   When the current user first accessed this shared schedule.
 */
data class SharedSchedule(
    val id: String = "",
    val scheduleId: String,
    val shareCode: String,
    val ownerName: String,
    /**
     * Local-only alias chosen by the viewer to override [ownerName] in their UI.
     * Null means "use [ownerName] as-is". Never sent back to the owner.
     */
    val localAliasName: String? = null,
    val events: List<Event> = emptyList(),
    val isImported: Boolean = false,
    /**
     * Local-only flag: should events from this schedule render on the viewer's
     * main calendar (Month / Week / Day)?
     * - true (default): events appear in calendar.
     * - false: events stay in Room and continue to sync, but are filtered out
     *   of all calendar queries.
     *
     * Never propagated to Firestore — this is a per-viewer preference.
     */
    val isVisibleInCalendar: Boolean = true,
    val accessedAt: LocalDateTime = LocalDateTime.now(),
) {
    /** Name shown to the viewer — alias takes precedence over [ownerName]. */
    val displayName: String
        get() = localAliasName?.takeIf { it.isNotBlank() } ?: ownerName
}
