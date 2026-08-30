package com.example.mycutieschedule.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Re-arms every pending event reminder after the device has booted (or our
 * package has been replaced / updated).
 *
 * Why this exists:
 *   AlarmManager state is volatile — every scheduled alarm is wiped on power
 *   off / OS upgrade. Without re-arming, users would miss reminders for events
 *   created before the reboot.
 *
 * Why a BroadcastReceiver instead of WorkManager:
 *   - Re-scheduling is fast: read N rows from Room, call AlarmManager N times.
 *     A 10 ms `goAsync()` window is plenty for a typical N.
 *   - Avoids pulling in WorkManager just for one one-shot boot task.
 *   - The receiver itself is short-lived; we only need to keep it alive long
 *     enough to enqueue the rescheduling work on [appScope].
 *
 * The receiver is registered for:
 *   BOOT_COMPLETED         — standard cold-boot signal
 *   QUICKBOOT_POWERON      — HTC/some Samsung devices use this
 *   LOCKED_BOOT_COMPLETED  — fires before user unlocks (for direct-boot apps)
 *   MY_PACKAGE_REPLACED    — alarms are also wiped when our app is updated
 */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject lateinit var eventDao: EventDao

    @Inject lateinit var reminderScheduler: EventReminderScheduler

    // `@field:` target ensures the qualifier is applied to the backing field,
    // which is what Hilt's field injection looks at for `lateinit var`.
    @Inject @field:ApplicationScope lateinit var appScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val relevant = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        if (!relevant) {
            Timber.d("BootCompletedReceiver: ignoring action=$action")
            return
        }

        // goAsync() lets the receiver outlive its synchronous onReceive() call
        // — without this, the launched coroutine would be killed almost
        // immediately when the BroadcastReceiver instance is torn down.
        val pending = goAsync()
        appScope.launch(Dispatchers.IO) {
            try {
                val events = findPendingReminderEvents(eventDao)
                Timber.d("BootCompletedReceiver: rescheduling ${events.size} reminder(s)")
                events.forEach { entity ->
                    reminderScheduler.schedule(entity.toDomain())
                }
            } catch (e: Exception) {
                Timber.e(e, "BootCompletedReceiver: rescheduling failed")
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Reads every event whose start is still in the future and that carries a
 * reminder, so [BootCompletedReceiver] can re-arm its alarm.
 *
 * Extracted from the receiver purely so it can be unit-tested: a
 * [BroadcastReceiver] cannot be exercised on the JVM, but this can.
 *
 * ## Why `LocalDateTime.now()` and not `System.currentTimeMillis()`
 *
 * The `start_datetime` column is **not** a true UTC epoch. It is written by
 * [toEpochMilli], which takes the user's wall-clock [LocalDateTime] and
 * reinterprets it as UTC — so the stored number is `trueEpoch + zoneOffset`.
 *
 * Feeding a real epoch (`System.currentTimeMillis()`) into a query that
 * compares against that column therefore shifts the filter by exactly the
 * device's UTC offset:
 *
 *  - **East of UTC** (e.g. UTC+3) the filter is over-inclusive: it also returns
 *    events that started up to 3 hours ago. Harmless in practice, because
 *    [EventReminderScheduler.schedule] drops triggers already in the past.
 *  - **West of UTC** (e.g. UTC−5) the filter is under-inclusive: it effectively
 *    demands `trueStart > trueNow + 5h`, so every reminder due within the next
 *    ~5 hours is silently *not* re-armed after a reboot. That is the bug.
 *
 * `LocalDateTime.now().toEpochMilli()` encodes "now" the same way the column
 * encodes event starts, so the comparison happens in one consistent time base.
 *
 * [EventReminderScheduler.schedule] stays as it is — it converts via
 * `ZoneId.systemDefault()` to a genuine epoch before handing the value to
 * AlarmManager, so comparing *its* trigger against `System.currentTimeMillis()`
 * is correct. Only this DAO-query input was in the wrong base.
 */
internal suspend fun findPendingReminderEvents(eventDao: EventDao): List<EventEntity> {
    val nowInStorageEncoding = LocalDateTime.now().toEpochMilli()
    return eventDao.findEventsWithFutureReminders(nowInStorageEncoding)
}
