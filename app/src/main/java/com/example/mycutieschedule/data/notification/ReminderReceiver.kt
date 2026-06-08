package com.example.mycutieschedule.data.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.mycutieschedule.MainActivity
import com.example.mycutieschedule.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * BroadcastReceiver invoked by [android.app.AlarmManager] when a scheduled
 * reminder time arrives. Builds and posts a Material 3-styled notification
 * for the event.
 *
 * Why not look up the event from Room here?
 *   The relevant fields (title, description, start/end) are passed via Intent
 *   extras at schedule time. Avoiding a DB read inside [onReceive] keeps the
 *   receiver fast and lets it work even before Hilt's deferred entry points
 *   are warm.
 *
 * Notification taps deep-link to [MainActivity]. We don't currently encode a
 * specific destination — `MainActivity` lands on the user's default tab,
 * which gives the user direct context to find the event in their calendar.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId       = intent.getStringExtra(ReminderConstants.EXTRA_EVENT_ID) ?: return
        val title         = intent.getStringExtra(ReminderConstants.EXTRA_TITLE).orEmpty()
        val description   = intent.getStringExtra(ReminderConstants.EXTRA_DESCRIPTION).orEmpty()
        val startEpoch    = intent.getLongExtra(ReminderConstants.EXTRA_START_EPOCH, -1L)
        val minutesBefore = intent.getIntExtra(ReminderConstants.EXTRA_MINUTES_BEFORE, 0)

        Timber.d("ReminderReceiver: firing for eventId=$eventId title=$title")

        // ── Runtime permission gate (Android 13+) ────────────────────────────
        // Receiving the alarm doesn't grant us notification permission. If the
        // user denied POST_NOTIFICATIONS, silently bail — Android will already
        // have suppressed the notification anyway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Timber.w("ReminderReceiver: POST_NOTIFICATIONS denied — suppressing notification")
            return
        }

        val notification = buildNotification(
            context        = context,
            title          = title,
            description    = description,
            startEpoch     = startEpoch,
            minutesBefore  = minutesBefore,
            eventId        = eventId,
        )

        // Notification ID derived from the event hash so re-firing the same
        // reminder (e.g. after edit + reschedule) updates rather than stacks.
        val notificationId = ReminderConstants.NOTIFICATION_ID_OFFSET + eventId.hashCode()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun buildNotification(
        context: Context,
        title: String,
        description: String,
        startEpoch: Long,
        minutesBefore: Int,
        eventId: String,
    ): android.app.Notification {
        // Subtitle: "В 14:30 · через 15 мин" or "Сейчас" if minutesBefore = 0
        val subtitle = buildSubtitle(startEpoch, minutesBefore)

        // Tap intent — open the app on the user's home tab.
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPi = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, ReminderConstants.CHANNEL_EVENT_REMINDERS)
            // Use the launcher icon as a safe fallback — the project's
            // res/mipmap-* set has ic_launcher available everywhere.
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title.ifBlank { "Напоминание" })
            .setContentText(if (description.isNotBlank()) description else subtitle)
            .setSubText(subtitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        buildString {
                            append(subtitle)
                            if (description.isNotBlank()) {
                                append('\n').append(description)
                            }
                        }
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
            .build()
    }

    private fun buildSubtitle(startEpoch: Long, minutesBefore: Int): String {
        if (startEpoch <= 0L) return ""
        val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startEpoch), ZoneId.systemDefault())
        val hhmm  = start.format(DateTimeFormatter.ofPattern("HH:mm"))
        return when {
            minutesBefore <= 0      -> "Сейчас · $hhmm"
            minutesBefore < 60      -> "Через $minutesBefore мин · в $hhmm"
            minutesBefore % 60 == 0 -> "Через ${minutesBefore / 60} ч · в $hhmm"
            else                    -> "Через ~${minutesBefore / 60} ч · в $hhmm"
        }
    }
}
