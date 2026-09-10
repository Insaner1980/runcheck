package com.runcheck.domain.repository

fun interface MonitoringAlertStateRepository {
    suspend fun clearAlertState()
}
