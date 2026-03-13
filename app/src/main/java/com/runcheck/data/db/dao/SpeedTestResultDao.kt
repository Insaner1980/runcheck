package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.SpeedTestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestResultDao {

    @Insert
    suspend fun insert(result: SpeedTestResultEntity): Long

    @Query("SELECT * FROM speed_test_results WHERE timestamp <= :now ORDER BY timestamp DESC LIMIT 1")
    fun getLatestResult(now: Long): Flow<SpeedTestResultEntity?>

    @Query("SELECT * FROM speed_test_results WHERE timestamp <= :now ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentResults(limit: Int, now: Long): Flow<List<SpeedTestResultEntity>>

    @Query("SELECT * FROM speed_test_results ORDER BY timestamp DESC")
    fun getAllResults(): Flow<List<SpeedTestResultEntity>>

    @Query("SELECT * FROM speed_test_results WHERE timestamp BETWEEN :since AND :now ORDER BY timestamp DESC")
    fun getResultsSince(since: Long, now: Long): Flow<List<SpeedTestResultEntity>>

    @Query("DELETE FROM speed_test_results WHERE id NOT IN (SELECT id FROM speed_test_results ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun deleteOldResults(keepCount: Int)

    @Query("DELETE FROM speed_test_results WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("SELECT COUNT(*) FROM speed_test_results")
    suspend fun getCount(): Int

    @Query("SELECT * FROM speed_test_results ORDER BY timestamp ASC")
    suspend fun getAll(): List<SpeedTestResultEntity>
}
