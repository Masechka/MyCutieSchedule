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
    val events: List<Event> = emptyList(),
    val isImported: Boolean = false,
    val accessedAt: LocalDateTime = LocalDateTime.now(),
)
