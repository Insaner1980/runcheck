package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.ThermalReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ThermalReadingDao {

    @Insert
    suspend fun insert(reading: ThermalReadingEntity)

    @Query("SELECT * FROM thermal_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getReadingsSince(since: Long): Flow<List<ThermalReadingEntity>>

    @Query("SELECT * FROM thermal_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<ThermalReadingEntity?>

    @Query("DELETE FROM thermal_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM thermal_readings")
    suspend fun getCount(): Int
}
