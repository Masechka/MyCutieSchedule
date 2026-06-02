package com.example.mycutieschedule.data.notification

/**
 * Single source of truth for notification & alarm intent keys.
 *
 * Keeping these as `const val` instead of literal strings prevents typos when
 * setting / reading extras across [ReminderReceiver], [EventReminderScheduler],
 * and [BootCompletedReceiver].
 */
internal object ReminderConstants {

    /** Notification channel for per-event reminders (Android 8+). */
    const val CHANNEL_EVENT_REMINDERS = "event_reminders"

    /** Stable numeric base for reminder notification IDs (we add eventId hash). */
    const val NOTIFICATION_ID_OFFSET = 1_000_000

    // ── Intent extras carried from scheduler → ReminderReceiver ───────────────
    const val EXTRA_EVENT_ID       = "extra_event_id"
    const val EXTRA_TITLE          = "extra_event_title"
    const val EXTRA_DESCRIPTION    = "extra_event_description"
    const val EXTRA_START_EPOCH    = "extra_event_start_epoch"
    const val EXTRA_END_EPOCH      = "extra_event_end_epoch"
    const val EXTRA_MINUTES_BEFORE = "extra_minutes_before"
}
