package com.runcheck.domain.model

data class MonitoringHeartbeat(
    val recordedAtEpochMillis: Long,
    val recordedAtUptimeMillis: Long,
    val intervalMinutes: Int,
)
