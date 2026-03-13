package com.devicepulse.domain.repository

import com.devicepulse.domain.model.SpeedTestProgress
import com.devicepulse.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow

interface SpeedTestRepository {
    fun runSpeedTest(): Flow<SpeedTestProgress>
    suspend fun saveResult(result: SpeedTestResult)
    fun getLatestResult(): Flow<SpeedTestResult?>
    fun getRecentResults(limit: Int): Flow<List<SpeedTestResult>>
    suspend fun trimResults(keepCount: Int)
    suspend fun deleteOlderThan(cutoff: Long)
}
