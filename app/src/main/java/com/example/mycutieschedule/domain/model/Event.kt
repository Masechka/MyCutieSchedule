package com.example.mycutieschedule.domain.model

import java.time.LocalDateTime

/**
 * Core domain model representing a calendar event.
 *
 * Business rules enforced by use cases (not here):
 *   - [title] must not be blank
 *   - [startDateTime] must be strictly before [endDateTime]
 *   - [color] must be one of the predefined [EventColor] values
 *
 * Firestore path: events/{id}
 * Room table:     event_table
 *
 * @param id               Unique identifier. Empty string = new event (assigned by AddEventUseCase).
 * @param title            Short name displayed on the calendar tile.
 * @param description      Optional longer description shown in the event detail screen.
 * @param startDateTime    Inclusive start.
 * @param endDateTime      Exclusive end. Must be after [startDateTime].
 * @param color            One of the predefined pink shades.
 * @param reminder         Optional notification reminder before the event.
 * @param recurrence       Optional repeat rule. Null = single occurrence.
 * @param ownerId                 Firebase Auth UID of the event creator.
 * @param isImported              True if this event was copied from another user's schedule.
 * @param sourceScheduleId        Share code / schedule ID this was imported from (if [isImported]).
 * @param recurrenceGroupId       Shared ID linking all events in a recurrence chain (master + generated).
 *                                Null for one-off events that don't repeat.
 * @param isRecurrenceGenerated   True when this event was automatically generated from a master recurring event.
 *                                The master itself has this set to false.
 * @param createdAt               Creation timestamp.
 * @param updatedAt               Last update timestamp — used for Firestore conflict resolution.
 */
data class Event(
    val id: String = "",
    val title: String,
    val description: String = "",
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val color: EventColor = EventColor.PEONY,
    val reminder: Reminder? = null,
    val recurrence: Recurrence? = null,
    val ownerId: String,
    val isImported: Boolean = false,
    val sourceScheduleId: String? = null,
    val recurrenceGroupId: String? = null,
    val isRecurrenceGenerated: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
