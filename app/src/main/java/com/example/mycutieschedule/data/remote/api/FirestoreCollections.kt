package com.example.mycutieschedule.data.remote.api

/**
 * Firestore path constants — single source of truth for all collection names and field keys.
 *
 * Collection layout:
 *   users/{userId}                   → UserDto document
 *   users/{userId}/events/{eventId}  → EventDto documents (subcollection)
 *   sharedSchedules/{shareCode}      → SharedScheduleDto documents
 *
 * Why a subcollection for events?
 *   Security rules can restrict `users/{userId}/events` to the owner alone,
 *   without exposing the top-level events collection to other users.
 */
object FirestoreCollections {

    /** Top-level user profile documents: `users/{userId}`. */
    const val USERS = "users"

    /** Events subcollection nested under each user: `users/{userId}/events/{eventId}`. */
    const val EVENTS = "events"

    /** Share code lookup documents: `sharedSchedules/{shareCode}`. */
    const val SHARED_SCHEDULES = "sharedSchedules"

    object UserFields {
        const val EMAIL        = "email"
        const val DISPLAY_NAME = "displayName"
        const val CREATED_AT   = "createdAt"
        const val SHARE_CODE   = "shareCode"
    }

    object EventFields {
        const val OWNER_ID   = "ownerId"
        const val UPDATED_AT = "updatedAt"
    }

    object SharedScheduleFields {
        const val USER_ID    = "userId"
        const val OWNER_NAME = "ownerName"
        const val SHARE_CODE = "shareCode"
        const val CREATED_AT = "createdAt"
    }
}
