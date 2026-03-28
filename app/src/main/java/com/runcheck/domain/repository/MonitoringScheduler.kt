package com.runcheck.domain.repository

import com.runcheck.domain.model.MonitoringInterval

interface MonitoringScheduler {
    fun schedule(interval: MonitoringInterval)

    fun cancel()

    suspend fun ensureScheduled()
}
