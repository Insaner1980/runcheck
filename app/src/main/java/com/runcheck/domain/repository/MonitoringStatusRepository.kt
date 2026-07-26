package com.runcheck.domain.repository

import com.runcheck.domain.model.MonitoringHeartbeat
import kotlinx.coroutines.flow.Flow

interface MonitoringStatusRepository {
    fun observeLastWorkerHeartbeat(): Flow<MonitoringHeartbeat?>

    suspend fun setLastWorkerHeartbeat(heartbeat: MonitoringHeartbeat)
}
