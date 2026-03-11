package com.devicepulse.domain.repository

import com.devicepulse.domain.model.ThrottlingEvent
import kotlinx.coroutines.flow.Flow

interface ThrottlingRepository {
    fun getRecentEvents(limit: Int = 50): Flow<List<ThrottlingEvent>>
    suspend fun insert(event: ThrottlingEvent)
    suspend fun deleteOlderThan(cutoff: Long)
}
