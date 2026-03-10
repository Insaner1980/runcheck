package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.ThrottlingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThrottlingEventDao {

    @Insert
    suspend fun insert(event: ThrottlingEventEntity)

    @Query("SELECT * FROM throttling_events WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<ThrottlingEventEntity>>

    @Query("SELECT * FROM throttling_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<ThrottlingEventEntity>>

    @Query("DELETE FROM throttling_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
