package com.example.mycutieschedule.data.mapper

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toLocalDate
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.data.local.entity.DayNoteEntity
import com.example.mycutieschedule.domain.model.DayNote

fun DayNoteEntity.toDomain(): DayNote = DayNote(
    id        = id,
    date      = dateEpoch.toLocalDate(),
    content   = content,
    ownerId   = ownerId,
    updatedAt = updatedAt.toLocalDateTime(),
)

fun DayNote.toEntity(): DayNoteEntity = DayNoteEntity(
    id        = id,
    dateEpoch = date.toEpochDay(),
    content   = content,
    ownerId   = ownerId,
    updatedAt = updatedAt.toEpochMilli(),
)
