package com.example.mycutieschedule.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.ReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules + cancels OS-level alarms that fire the [ReminderReceiver] when
 * an event's reminder time arrives.
 *
 * Algorithm:
 *   triggerEpoch = (event.startDateTime - reminder.minutesBefore) interpreted
 *                  as a wall-clock time in the device's current zone.
 *
 * Storage convention: [Event.startDateTime] is a naive [java.time.LocalDateTime]
 * — the project stores wall-clock time. We resolve it against
 * [ZoneId.systemDefault] here because AlarmManager.RTC_WAKEUP demands real
 * epoch milliseconds in the device's actual timezone.
 *
 * Battery & exactness:
 *   - On Android 12+ (S+) we try `setExactAndAllowWhileIdle` because reminders
 *     are time-sensitive and the calendar use case qualifies for
 *     USE_EXACT_ALARM (auto-granted on 13+).
 *   - On Android 12 the user MAY revoke SCHEDULE_EXACT_ALARM in system
 *     Settings. `canScheduleExactAlarms()` is checked; on denial we degrade
 *     to `setAndAllowWhileIdle` (Doze-friendly, accuracy ~few minutes).
 *   - On pre-S devices `setExactAndAllowWhileIdle` is unconditionally available.
 *
 * Idempotent: re-scheduling the same eventId replaces the previous alarm via
 * [PendingIntent.FLAG_UPDATE_CURRENT]. Callers can `schedule(event)` after
 * every edit without checking what was already scheduled.
 *
 * NEVER throws for routine reasons (past trigger, no reminder, wrong type,
 * permission revoked) — just logs and returns. Failure to schedule must not
 * crash the save path.
 */
@Singleton
class EventReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val alarmManager: AlarmManager? = context.getSystemService()

    /**
     * Schedules a reminder for [event] if it has one. Cancels any previously
     * scheduled alarm for the same event ID first (handled implicitly by
     * `FLAG_UPDATE_CURRENT`).
     *
     * Returns silently if:
     *   - reminder is null or wrong type
     *   - the trigger time has already passed
     *   - AlarmManager is unavailable
     *   - imported events (cannot schedule for someone else's calendar)
     */
    fun schedule(event: Event) {
        if (event.isImported) return                       // viewer-only, no scheduling
        val am = alarmManager ?: return
        val reminder = event.reminder ?: return
        if (reminder.type != ReminderType.NOTIFICATION) return

        val triggerEpoch = event.startDateTime
            .minusMinutes(reminder.minutesBefore.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerEpoch <= System.currentTimeMillis()) {
            // Past — never schedule retroactive alarms.
            Timber.d(
                "EventReminderScheduler: skip past reminder for eventId=${event.id} " +
                    "(trigger=$triggerEpoch, now=${System.currentTimeMillis()})"
            )
            return
        }

        val pi = buildPendingIntent(event)

        try {
            if (canUseExact(am)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pi)
                Timber.d(
                    "EventReminderScheduler: EXACT alarm for eventId=${event.id} " +
                        "title=${event.title} at $triggerEpoch"
                )
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pi)
                Timber.d(
                    "EventReminderScheduler: INEXACT alarm for eventId=${event.id} " +
                        "title=${event.title} at $triggerEpoch (exact permission denied)"
                )
            }
        } catch (e: SecurityException) {
            // Defensive: SCHEDULE_EXACT_ALARM can be revoked between our check
            // and the call. Fall back to inexact, swallow the error.
            Timber.w(e, "EventReminderScheduler: exact alarm denied, falling back to inexact")
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerEpoch, pi)
        }
    }

    /**
     * Cancels a previously-scheduled alarm for [eventId]. Safe to call even if
     * no alarm exists — AlarmManager.cancel is idempotent.
     */
    fun cancel(eventId: String) {
        val am = alarmManager ?: return
        // FLAG_NO_CREATE: returns null if no matching PendingIntent exists, so
        // we don't accidentally create a phantom one just to cancel it.
        val pi = PendingIntent.getBroadcast(
            context,
            requestCodeFor(eventId),
            baseIntent(eventId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: run {
            Timber.d("EventReminderScheduler: cancel — no alarm for eventId=$eventId")
            return
        }
        am.cancel(pi)
        pi.cancel()
        Timber.d("EventReminderScheduler: cancelled alarm for eventId=$eventId")
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /** Stable, distinct PendingIntent for each event. Hashcode collisions are
     *  astronomically unlikely with UUIDs but the `eventId` extra disambiguates
     *  inside [ReminderReceiver] if one ever occurs. */
    private fun requestCodeFor(eventId: String): Int = eventId.hashCode()

    private fun baseIntent(eventId: String): Intent =
        Intent(context, ReminderReceiver::class.java).apply {
            // Action makes the Intent unique per-event in the system's view,
            // preventing PendingIntent.getBroadcast from returning a cached
            // intent for an unrelated event with the same component.
            action = "com.example.mycutieschedule.REMINDER_$eventId"
        }

    private fun buildPendingIntent(event: Event): PendingIntent {
        val intent = baseIntent(event.id).apply {
            putExtra(ReminderConstants.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderConstants.EXTRA_TITLE, event.title)
            putExtra(ReminderConstants.EXTRA_DESCRIPTION, event.description)
            putExtra(
                ReminderConstants.EXTRA_START_EPOCH,
                event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            putExtra(
                ReminderConstants.EXTRA_END_EPOCH,
                event.endDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            )
            putExtra(ReminderConstants.EXTRA_MINUTES_BEFORE, event.reminder?.minutesBefore ?: 0)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(event.id),
            intent,
            // UPDATE_CURRENT lets us replace the previous alarm with new extras
            // (e.g. user edited the title). IMMUTABLE is required on 31+.
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canUseExact(am: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true   // pre-S: setExactAndAllowWhileIdle is always available
        }
    }
}
