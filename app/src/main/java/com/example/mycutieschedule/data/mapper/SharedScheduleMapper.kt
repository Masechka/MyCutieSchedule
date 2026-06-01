package com.example.mycutieschedule.data.mapper

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.data.local.entity.SharedScheduleEntity
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.SharedSchedule

/**
 * Maps [SharedScheduleEntity] to [SharedSchedule].
 *
 * Events are NOT stored inside SharedScheduleEntity — they live in event_table
 * with [EventEntity.isImported] = true. The caller must supply the event list.
 */
fun SharedScheduleEntity.toDomain(events: List<Event> = emptyList()): SharedSchedule =
    SharedSchedule(
        id                  = id,
        scheduleId          = scheduleId,
        shareCode           = shareCode,
        ownerName           = ownerName,
        localAliasName      = localAliasName,
        events              = events,
        isImported          = isImported,
        isVisibleInCalendar = isVisibleInCalendar,
        accessedAt          = accessedAt.toLocalDateTime(),
    )

fun SharedSchedule.toEntity(viewerId: String): SharedScheduleEntity =
    SharedScheduleEntity(
        id                  = id,
        scheduleId          = scheduleId,
        shareCode           = shareCode,
        ownerName           = ownerName,
        localAliasName      = localAliasName,
        viewerId            = viewerId,
        isImported          = isImported,
        isVisibleInCalendar = isVisibleInCalendar,
        accessedAt          = accessedAt.toEpochMilli(),
    )
