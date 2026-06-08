package com.example.mycutieschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycutieschedule.data.local.entity.DayNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DayNoteDao {

    /**
     * Observe the note for a specific calendar day.
     * Emits null if no note exists for that day.
     *
     * @param dateEpoch [java.time.LocalDate.toEpochDay] value.
     */
    @Query("""
        SELECT * FROM day_note_table
        WHERE date_epoch = :dateEpoch
          AND owner_id = :userId
        LIMIT 1
    """)
    fun observeByDate(dateEpoch: Long, userId: String): Flow<DayNoteEntity?>

    @Query("SELECT * FROM day_note_table WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): DayNoteEntity?

    /**
     * Upsert: if a note for this (date, owner) pair already exists, it is replaced.
     * The unique index on (date_epoch, owner_id) triggers the REPLACE strategy.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: DayNoteEntity)

    @Query("DELETE FROM day_note_table WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM day_note_table WHERE date_epoch = :dateEpoch AND owner_id = :userId")
    suspend fun deleteByDate(dateEpoch: Long, userId: String)
}
