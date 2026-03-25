package com.runcheck.domain.repository

import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    fun runSpeedTest(): Flow<SpeedTestProgress>
    suspend fun saveResult(result: SpeedTestResult)
    fun getLatestResult(): Flow<SpeedTestResult?>
    fun getRecentResults(limit: Int): Flow<List<SpeedTestResult>>
    suspend fun trimResults(keepCount: Int)
    suspend fun deleteOlderThan(cutoff: Long)
    suspend fun deleteAll()
}
