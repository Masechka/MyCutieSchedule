package com.example.mycutieschedule.data.mapper

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toLocalDate
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.data.remote.dto.EventDto
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.model.Reminder
import com.example.mycutieschedule.domain.model.ReminderType
import java.time.DayOfWeek

// ── Dto → Domain ──────────────────────────────────────────────────────────────

/**
 * Converts a [EventDto] received from Firestore into the domain [Event].
 *
 * Defensive mapping: unknown enum values fall back to sensible defaults so
 * a future schema change (e.g. a new EventColor added in a later version of
 * the app) doesn't crash an older install.
 */
fun EventDto.toDomain(): Event = Event(
    id               = id,
    title            = title,
    description      = description,
    startDateTime    = startDateTime.toLocalDateTime(),
    endDateTime      = endDateTime.toLocalDateTime(),
    color            = EventColor.entries.find { it.name == color } ?: EventColor.PEONY,
    reminder         = buildDtoReminder(reminderMinutesBefore, reminderType),
    recurrence       = buildDtoRecurrence(
        recurrenceType,
        recurrenceRepeatDays,
        recurrenceEndDate,
        recurrenceCount,
    ),
    ownerId          = ownerId,
    isImported       = isImported,
    sourceScheduleId = sourceScheduleId,
    createdAt        = createdAt.toLocalDateTime(),
    updatedAt        = updatedAt.toLocalDateTime(),
)

private fun buildDtoReminder(minutesBefore: Int?, typeName: String?): Reminder? {
    if (minutesBefore == null || typeName == null) return null
    return Reminder(
        minutesBefore = minutesBefore,
        type = ReminderType.entries.find { it.name == typeName } ?: ReminderType.NOTIFICATION,
    )
}

private fun buildDtoRecurrence(
    typeName: String?,
    repeatDays: String?,
    endDateEpoch: Long?,
    count: Int?,
): Recurrence? {
    if (typeName == null) return null
    val type = RecurrenceType.entries.find { it.name == typeName } ?: return null
    return Recurrence(
        type = type,
        repeatDays = repeatDays
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { runCatching { DayOfWeek.valueOf(it) }.getOrNull() }
            ?.toSet()
            ?: emptySet(),
        endDate = endDateEpoch?.toLocalDate(),
        count   = count,
    )
}

// ── Domain → Dto ──────────────────────────────────────────────────────────────

/**
 * Converts a domain [Event] into an [EventDto] ready to be written to Firestore.
 */
fun Event.toDto(): EventDto = EventDto(
    id                    = id,
    title                 = title,
    description           = description,
    startDateTime         = startDateTime.toEpochMilli(),
    endDateTime           = endDateTime.toEpochMilli(),
    color                 = color.name,
    reminderMinutesBefore = reminder?.minutesBefore,
    reminderType          = reminder?.type?.name,
    recurrenceType        = recurrence?.type?.name,
    recurrenceRepeatDays  = recurrence?.repeatDays
        ?.joinToString(",") { it.name }
        ?.takeIf { it.isNotEmpty() },
    recurrenceEndDate     = recurrence?.endDate?.toEpochDay(),
    recurrenceCount       = recurrence?.count,
    ownerId               = ownerId,
    isImported            = isImported,
    sourceScheduleId      = sourceScheduleId,
    createdAt             = createdAt.toEpochMilli(),
    updatedAt             = updatedAt.toEpochMilli(),
)
