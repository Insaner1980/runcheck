package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.NetworkReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkReadingDao {

    @Insert
    suspend fun insert(reading: NetworkReadingEntity)

    @Query("SELECT * FROM network_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getReadingsSince(since: Long): Flow<List<NetworkReadingEntity>>

    @Query("SELECT * FROM network_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<NetworkReadingEntity?>

    @Query("DELETE FROM network_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM network_readings")
    suspend fun getCount(): Int
}
