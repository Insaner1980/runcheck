package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.runcheck.data.db.entity.NetworkReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkReadingDao {

    @Insert
    suspend fun insert(reading: NetworkReadingEntity)

    @Query("SELECT * FROM network_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getReadingsSince(since: Long): Flow<List<NetworkReadingEntity>>

    @Query(
        """
            SELECT * FROM network_readings
            WHERE id IN (
                SELECT id FROM network_readings
                WHERE timestamp >= :since
                ORDER BY timestamp DESC
                LIMIT :limit
            )
            ORDER BY timestamp ASC
        """
    )
    fun getReadingsSinceLimited(since: Long, limit: Int): Flow<List<NetworkReadingEntity>>

    @Query("SELECT * FROM network_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<NetworkReadingEntity?>

    @Query("DELETE FROM network_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("DELETE FROM network_readings")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM network_readings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM network_readings ORDER BY timestamp ASC")
    suspend fun getAll(): List<NetworkReadingEntity>
}
