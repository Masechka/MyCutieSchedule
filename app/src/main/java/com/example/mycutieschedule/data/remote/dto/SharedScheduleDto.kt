package com.example.mycutieschedule.data.remote.dto

/**
 * Firestore document that maps a share code to a user's schedule.
 *
 * This document exists in `sharedSchedules/{shareCode}` and acts as a lookup
 * table: given a code, we can find the owner's userId and display name,
 * then load their events from `users/{userId}/events`.
 *
 * **All fields must have default values** for Firebase reflection-based deserialization.
 *
 * Firestore path: `sharedSchedules/{shareCode}`
 */
data class SharedScheduleDto(

    /** Firebase Auth UID of the schedule owner. */
    val userId: String = "",

    /** Display name of the owner — shown in the "shared schedules" list UI. */
    val ownerName: String = "",

    /** The 6-character alphanumeric code that was used to create this entry. */
    val shareCode: String = "",

    /** UTC epoch milliseconds — when the share code was generated. */
    val createdAt: Long = 0L,
)
