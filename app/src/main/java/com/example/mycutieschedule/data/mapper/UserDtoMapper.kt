package com.example.mycutieschedule.data.mapper

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.data.remote.dto.UserDto
import com.example.mycutieschedule.domain.model.User

// ── Dto → Domain ──────────────────────────────────────────────────────────────

fun UserDto.toDomain(): User = User(
    id          = id,
    email       = email,
    displayName = displayName,
    createdAt   = createdAt.toLocalDateTime(),
)

// ── Domain → Dto ──────────────────────────────────────────────────────────────

/**
 * @param shareCode The user's current share code, if any.
 *   Kept as a separate parameter (not part of [User]) because the domain
 *   model doesn't expose share codes — that's infrastructure detail.
 */
fun User.toDto(shareCode: String? = null): UserDto = UserDto(
    id          = id,
    email       = email,
    displayName = displayName,
    createdAt   = createdAt.toEpochMilli(),
    shareCode   = shareCode,
)
