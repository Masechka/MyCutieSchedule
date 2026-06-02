package com.example.mycutieschedule.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.di.ApplicationScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
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
                val now = System.currentTimeMillis()
                val events = eventDao.findEventsWithFutureReminders(now)
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
