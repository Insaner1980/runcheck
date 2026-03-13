package com.devicepulse.domain.repository

import com.devicepulse.domain.model.DataRetention
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAmoledBlack(enabled: Boolean)
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun setMonitoringInterval(interval: MonitoringInterval)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDataRetention(retention: DataRetention)
    suspend fun setCrashReportingEnabled(enabled: Boolean)
    fun getPermissionEducationSeen(): Flow<Boolean>
    suspend fun setPermissionEducationSeen(seen: Boolean)
    suspend fun getAppUsageLastCollectedAt(): Long?
    suspend fun setAppUsageLastCollectedAt(timestamp: Long)
}
