package com.example.mycutieschedule.data.mapper

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toLocalDate
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.model.Reminder
import com.example.mycutieschedule.domain.model.ReminderType
import java.time.DayOfWeek
import java.time.LocalDate

// ── Entity → Domain ───────────────────────────────────────────────────────────

fun EventEntity.toDomain(): Event = Event(
    id                     = id,
    title                  = title,
    description            = description,
    startDateTime          = startDatetime.toLocalDateTime(),
    endDateTime            = endDatetime.toLocalDateTime(),
    color                  = EventColor.valueOf(color),
    reminder               = buildReminder(reminderMinutesBefore, reminderType),
    recurrence             = buildRecurrence(
        recurrenceType,
        recurrenceRepeatDays,
        recurrenceEndDate,
        recurrenceCount,
        recurrenceIntervalWeeks,
        recurrenceCustomDates,
    ),
    ownerId                = ownerId,
    isImported             = isImported,
    sourceScheduleId       = sourceScheduleId,
    recurrenceGroupId      = recurrenceGroupId,
    isRecurrenceGenerated  = isRecurrenceGenerated,
    createdAt              = createdAt.toLocalDateTime(),
    updatedAt              = updatedAt.toLocalDateTime(),
)

private fun buildReminder(minutesBefore: Int?, typeName: String?): Reminder? {
    if (minutesBefore == null || typeName == null) return null
    return Reminder(
        minutesBefore = minutesBefore,
        type          = ReminderType.valueOf(typeName),
    )
}

private fun buildRecurrence(
    typeName: String?,
    repeatDays: String?,
    endDateEpoch: Long?,
    count: Int?,
    intervalWeeks: Int?,
    customDates: String?,
): Recurrence? {
    if (typeName == null) return null
    return Recurrence(
        type          = RecurrenceType.valueOf(typeName),
        intervalWeeks = intervalWeeks ?: 1,
        repeatDays    = repeatDays
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { DayOfWeek.valueOf(it) }
            ?.toSet()
            ?: emptySet(),
        customDates   = customDates
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.map { LocalDate.ofEpochDay(it.trim().toLong()) }
            ?: emptyList(),
        endDate       = endDateEpoch?.toLocalDate(),
        count         = count,
    )
}

// ── Domain → Entity ───────────────────────────────────────────────────────────

fun Event.toEntity(): EventEntity = EventEntity(
    id                      = id,
    title                   = title,
    description             = description,
    startDatetime           = startDateTime.toEpochMilli(),
    endDatetime             = endDateTime.toEpochMilli(),
    color                   = color.name,
    reminderMinutesBefore   = reminder?.minutesBefore,
    reminderType            = reminder?.type?.name,
    recurrenceType          = recurrence?.type?.name,
    recurrenceRepeatDays    = recurrence?.repeatDays
        ?.joinToString(",") { it.name }
        ?.takeIf { it.isNotEmpty() },
    recurrenceEndDate       = recurrence?.endDate?.toEpochDay(),
    recurrenceCount         = recurrence?.count,
    recurrenceIntervalWeeks = recurrence?.intervalWeeks?.takeIf { it > 1 },
    recurrenceCustomDates   = recurrence?.customDates
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(",") { it.toEpochDay().toString() },
    recurrenceGroupId       = recurrenceGroupId,
    isRecurrenceGenerated   = isRecurrenceGenerated,
    ownerId                 = ownerId,
    isImported              = isImported,
    sourceScheduleId        = sourceScheduleId,
    createdAt               = createdAt.toEpochMilli(),
    updatedAt               = updatedAt.toEpochMilli(),
)
