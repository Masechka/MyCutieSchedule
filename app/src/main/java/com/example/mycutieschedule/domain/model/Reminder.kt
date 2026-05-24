package com.example.mycutieschedule.domain.model

/**
 * Notification reminder attached to an event.
 *
 * @param minutesBefore How many minutes before the event start to trigger.
 *                      Common values: 0, 5, 10, 15, 30, 60, 120, 1440 (1 day).
 * @param type          Delivery mechanism for the reminder.
 */
data class Reminder(
    val minutesBefore: Int,
    val type: ReminderType = ReminderType.NOTIFICATION,
)

enum class ReminderType {
    /** Android notification via NotificationManager. */
    NOTIFICATION,

    /** Full-screen alarm intent (requires SCHEDULE_EXACT_ALARM permission on API 31+). */
    ALARM,
}
