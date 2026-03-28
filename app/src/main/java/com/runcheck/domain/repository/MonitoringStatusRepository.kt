package com.runcheck.domain.repository

import kotlinx.coroutines.flow.Flow

interface MonitoringStatusRepository {
    fun observeLastWorkerHeartbeatAt(): Flow<Long?>

    suspend fun setLastWorkerHeartbeatAt(timestamp: Long)
}
