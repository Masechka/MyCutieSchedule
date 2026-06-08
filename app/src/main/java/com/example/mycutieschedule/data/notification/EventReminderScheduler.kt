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
 * Планирует и отменяет OS-уровневые алармы [AlarmManager], которые запускают
 * [ReminderReceiver] в момент срабатывания напоминания о событии.
 *
 * **Алгоритм расчёта времени срабатывания:**
 *   triggerEpoch = (event.startDateTime − reminder.minutesBefore),
 *   интерпретированное как wall-clock-время в текущей таймзоне устройства.
 *
 * **Соглашение о хранении:** [Event.startDateTime] — это «голый»
 * [java.time.LocalDateTime] (без таймзоны), проект хранит wall-clock-время
 * (то, что видит пользователь). Здесь мы преобразуем его через
 * [ZoneId.systemDefault], потому что AlarmManager.RTC_WAKEUP требует
 * реальные epoch milliseconds в фактической таймзоне устройства.
 *
 * **Батарея и точность:**
 *   - Android 12+ (S+): пробуем `setExactAndAllowWhileIdle`, потому что
 *     напоминания time-sensitive, а календарные приложения подпадают под
 *     `USE_EXACT_ALARM` (авто-выдан на 13+).
 *   - Android 12: пользователь МОЖЕТ отозвать `SCHEDULE_EXACT_ALARM` в
 *     системных настройках. Проверяем `canScheduleExactAlarms()`; на отказе
 *     gracefully падаем на `setAndAllowWhileIdle` (Doze-friendly,
 *     точность ~несколько минут).
 *   - Pre-S: `setExactAndAllowWhileIdle` доступен безусловно.
 *
 * **Идемпотентность:** повторный schedule с тем же eventId заменяет
 * предыдущий аларм через [PendingIntent.FLAG_UPDATE_CURRENT]. Поэтому
 * репозиторий может вызывать `schedule(event)` после каждого edit без
 * предварительной проверки текущего состояния.
 *
 * **Никогда не throws** для рутинных причин (триггер в прошлом, нет
 * напоминания, неверный тип, отозванное разрешение) — только лог и return.
 * Падение планирования не должно крашить save-флоу пользователя.
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
