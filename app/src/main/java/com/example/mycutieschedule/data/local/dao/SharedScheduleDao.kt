package com.example.mycutieschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mycutieschedule.data.local.entity.SharedScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SharedScheduleDao {

    /** Live stream of all shared schedules accessed by [viewerId], newest first. */
    @Query("""
        SELECT * FROM shared_schedule_table
        WHERE viewer_id = :viewerId
        ORDER BY accessed_at DESC
    """)
    fun observeAll(viewerId: String): Flow<List<SharedScheduleEntity>>

    @Query("SELECT * FROM shared_schedule_table WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): SharedScheduleEntity?

    @Query("SELECT * FROM shared_schedule_table WHERE share_code = :shareCode AND viewer_id = :viewerId LIMIT 1")
    suspend fun findByShareCode(shareCode: String, viewerId: String): SharedScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: SharedScheduleEntity)

    /**
     * Updates only the [SharedScheduleEntity.localAliasName] field — the
     * viewer-chosen alias that overrides [SharedScheduleEntity.ownerName] in
     * the UI. Pass null to clear the alias and revert to the owner-provided name.
     */
    @Query("UPDATE shared_schedule_table SET local_alias_name = :alias WHERE id = :id")
    suspend fun updateAlias(id: String, alias: String?)

    /**
     * Toggles the calendar-visibility flag without touching the imported events
     * themselves. Background sync continues to apply owner changes regardless
     * of visibility — the flag is only consulted by calendar view filters.
     */
    @Query("UPDATE shared_schedule_table SET is_visible_in_calendar = :visible WHERE id = :id")
    suspend fun updateVisibility(id: String, visible: Boolean)

    @Query("DELETE FROM shared_schedule_table WHERE id = :id")
    suspend fun deleteById(id: String)
}
