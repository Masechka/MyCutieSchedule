package com.example.mycutieschedule.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates (and is responsible for the lifecycle of) the notification channels
 * used by the app.
 *
 * On Android 8+ every notification MUST be posted to a registered channel —
 * the channel decides default importance, vibration, sound, and lock-screen
 * visibility. The user can override these per-channel from system Settings.
 *
 * Idempotent: re-registering an existing channel updates only the mutable
 * fields (name, description) — the importance the user has chosen is never
 * downgraded, per Android's behavior. Safe to call on every cold launch.
 */
@Singleton
class ReminderNotificationChannel @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun ensureCreated() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = context.getSystemService<NotificationManager>() ?: return

        val channel = NotificationChannel(
            ReminderConstants.CHANNEL_EVENT_REMINDERS,
            "Напоминания о событиях",
            // IMPORTANCE_HIGH = pop-up heads-up + sound + vibrate. Reminders
            // are inherently time-sensitive so this is the right default —
            // users can downgrade in Settings if they prefer quiet.
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Уведомления о ваших запланированных событиях"
            enableVibration(true)
            setShowBadge(true)
        }

        nm.createNotificationChannel(channel)
    }
}
