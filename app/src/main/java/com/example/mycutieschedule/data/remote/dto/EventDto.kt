package com.example.mycutieschedule.data.remote.dto

/**
 * Firestore representation of a calendar event.
 *
 * **All fields must have default values** — Firestore's Android SDK requires a
 * no-argument-callable constructor to deserialize documents via reflection.
 *
 * Field names are intentionally identical to Firestore document keys (camelCase)
 * so no `@PropertyName` annotations are needed.
 *
 * Firestore path: `users/{ownerId}/events/{id}`
 * Maps to domain: [com.example.mycutieschedule.domain.model.Event]
 * Maps to entity: [com.example.mycutieschedule.data.local.entity.EventEntity]
 */
data class EventDto(

    val id: String = "",
    val title: String = "",
    val description: String = "",

    /** UTC epoch milliseconds. */
    val startDateTime: Long = 0L,

    /** UTC epoch milliseconds. */
    val endDateTime: Long = 0L,

    /** [com.example.mycutieschedule.domain.model.EventColor].name */
    val color: String = "PEONY",

    // ── Reminder ────────────────────────────────────────────────────────────────
    val reminderMinutesBefore: Int? = null,
    /** [com.example.mycutieschedule.domain.model.ReminderType].name */
    val reminderType: String? = null,

    // ── Recurrence ──────────────────────────────────────────────────────────────
    /** [com.example.mycutieschedule.domain.model.RecurrenceType].name */
    val recurrenceType: String? = null,
    /** Comma-separated [java.time.DayOfWeek].name values, e.g. "MONDAY,WEDNESDAY". */
    val recurrenceRepeatDays: String? = null,
    /** Epoch day via LocalDate.toEpochDay(). */
    val recurrenceEndDate: Long? = null,
    val recurrenceCount: Int? = null,

    // ── Ownership ───────────────────────────────────────────────────────────────
    val ownerId: String = "",
    val isImported: Boolean = false,
    val sourceScheduleId: String? = null,

    // ── Timestamps ──────────────────────────────────────────────────────────────
    /** UTC epoch milliseconds. */
    val createdAt: Long = 0L,

    /**
     * UTC epoch milliseconds.
     * Used for last-write-wins conflict resolution during Firestore ↔ Room sync.
     */
    val updatedAt: Long = 0L,
)
