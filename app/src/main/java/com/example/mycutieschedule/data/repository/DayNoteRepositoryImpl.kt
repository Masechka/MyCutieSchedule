package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.local.dao.DayNoteDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.domain.model.DayNote
import com.example.mycutieschedule.domain.repository.DayNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class DayNoteRepositoryImpl @Inject constructor(
    private val dayNoteDao: DayNoteDao,
) : DayNoteRepository {

    override fun observeByDate(date: LocalDate, userId: String): Flow<DayNote?> =
        dayNoteDao
            .observeByDate(dateEpoch = date.toEpochDay(), userId = userId)
            .map { it?.toDomain() }

    override suspend fun saveNote(note: DayNote): Result<Unit> = runCatching {
        dayNoteDao.upsert(note.toEntity())
    }.toDomainResult()

    override suspend fun deleteNote(date: LocalDate, userId: String): Result<Unit> = runCatching {
        dayNoteDao.deleteByDate(dateEpoch = date.toEpochDay(), userId = userId)
    }.toDomainResult()
}
