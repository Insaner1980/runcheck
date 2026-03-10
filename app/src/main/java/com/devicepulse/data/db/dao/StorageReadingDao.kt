package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.StorageReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageReadingDao {

    @Insert
    suspend fun insert(reading: StorageReadingEntity)

    @Query("SELECT * FROM storage_readings WHERE timestamp >= :since ORDER BY timestamp ASC")
    fun getReadingsSince(since: Long): Flow<List<StorageReadingEntity>>

    @Query("SELECT * FROM storage_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<StorageReadingEntity?>

    @Query("DELETE FROM storage_readings WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM storage_readings")
    suspend fun getCount(): Int

    @Query("SELECT * FROM storage_readings ORDER BY timestamp ASC")
    suspend fun getAll(): List<StorageReadingEntity>
}
