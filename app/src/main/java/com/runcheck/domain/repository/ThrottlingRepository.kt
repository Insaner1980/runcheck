package com.devicepulse.domain.repository

import com.devicepulse.domain.model.ThrottlingEvent
import kotlinx.coroutines.flow.Flow

interface ThrottlingRepository {
    fun getRecentEvents(limit: Int = 50): Flow<List<ThrottlingEvent>>
    suspend fun insert(event: ThrottlingEvent): Long
    suspend fun updateSnapshot(
        id: Long,
        thermalStatus: String,
        batteryTempC: Float,
        cpuTempC: Float?,
        foregroundApp: String?
    )
    suspend fun updateDuration(id: Long, durationMs: Long)
    suspend fun deleteOlderThan(cutoff: Long)
}
