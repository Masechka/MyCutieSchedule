package com.example.mycutieschedule.domain.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.DayNote
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contract for day note operations.
 * Implementation: data/repository/DayNoteRepositoryImpl.kt
 */
interface DayNoteRepository {

    /** Observe the note for a specific day. Emits null when no note exists. */
    fun observeByDate(date: LocalDate, userId: String): Flow<DayNote?>

    /** Save or update a note (upsert). */
    suspend fun saveNote(note: DayNote): Result<Unit>

    /** Delete the note for a specific day. */
    suspend fun deleteNote(date: LocalDate, userId: String): Result<Unit>
}
