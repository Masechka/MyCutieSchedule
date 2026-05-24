package com.example.mycutieschedule.domain.model

import java.time.LocalDateTime

/**
 * A user's personal schedule container.
 *
 * Each user has exactly one Schedule (1:1 with User).
 * The Schedule can be shared with other users via a [shareCode].
 *
 * Firestore path: userSchedules/{ownerId}
 *
 * @param id        Document ID — equals [ownerId] for simplicity.
 * @param ownerId   Firebase Auth UID of the schedule owner.
 * @param shareCode Short alphanumeric code used to share this schedule.
 *                  Null if the user has never generated a share link.
 * @param isShared  Whether the schedule is currently accessible via [shareCode].
 * @param createdAt Schedule creation time.
 * @param updatedAt Last modification time.
 */
data class Schedule(
    val id: String,
    val ownerId: String,
    val shareCode: String? = null,
    val isShared: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
