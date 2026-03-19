package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.runcheck.data.db.entity.BatteryReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryReadingDao {

    @Insert
    suspend fun insert(reading: BatteryReadingEntity)

    @Query("SELECT * FROM battery_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getReadingsSince(since: Long): Flow<List<BatteryReadingEntity>>

    @Query(
        """
            SELECT * FROM battery_readings
            WHERE id IN (
                SELECT id FROM battery_readings
                WHERE timestamp >= :since
                ORDER BY timestamp DESC
                LIMIT :limit
            )
            ORDER BY timestamp ASC
        """
    )
    fun getReadingsSinceLimited(since: Long, limit: Int): Flow<List<BatteryReadingEntity>>

    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<BatteryReadingEntity?>

    @Query("DELETE FROM battery_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM battery_readings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM battery_readings ORDER BY timestamp ASC")
    suspend fun getAll(): List<BatteryReadingEntity>

    @Query("SELECT * FROM battery_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    suspend fun getReadingsSinceSync(since: Long): List<BatteryReadingEntity>

    @Query("SELECT timestamp FROM battery_readings WHERE status = 'CHARGING' ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastChargingTimestamp(): Long?
}
