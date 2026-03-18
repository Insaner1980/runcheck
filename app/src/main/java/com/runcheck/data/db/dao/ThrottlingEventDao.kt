package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.runcheck.data.db.entity.ThrottlingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThrottlingEventDao {

    @Insert
    suspend fun insert(event: ThrottlingEventEntity): Long

    @Query("SELECT * FROM throttling_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<ThrottlingEventEntity>>

    @Query(
        """
        UPDATE throttling_events
        SET thermal_status = :thermalStatus,
            battery_temp_c = :batteryTempC,
            cpu_temp_c = :cpuTempC,
            foreground_app = COALESCE(:foregroundApp, foreground_app)
        WHERE id = :id
        """
    )
    suspend fun updateSnapshot(
        id: Long,
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    )

    @Query("UPDATE throttling_events SET duration_ms = :durationMs WHERE id = :id")
    suspend fun updateDuration(id: Long, durationMs: Long)

    @Query("DELETE FROM throttling_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
