package com.example.mycutieschedule.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A freeform text note attached to a specific calendar day.
 *
 * Displayed at the bottom of the Day View screen.
 * One note per user per day (upsert semantics).
 *
 * @param id        Unique identifier (Firestore document ID or Room primary key).
 * @param date      The calendar day this note belongs to.
 * @param content   The note text. Empty string means the note has been cleared.
 * @param ownerId   Firebase Auth UID of the note's author.
 * @param updatedAt Last modification time, used for conflict resolution during sync.
 */
data class DayNote(
    val id: String = "",
    val date: LocalDate,
    val content: String,
    val ownerId: String,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
