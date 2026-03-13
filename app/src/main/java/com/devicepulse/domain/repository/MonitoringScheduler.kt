package com.devicepulse.domain.repository

import com.devicepulse.domain.model.MonitoringInterval

interface MonitoringScheduler {
    fun schedule(interval: MonitoringInterval)
    fun cancel()
    suspend fun ensureScheduled()
}
