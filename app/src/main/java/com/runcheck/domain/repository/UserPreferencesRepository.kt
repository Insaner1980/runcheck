package com.runcheck.domain.repository

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.UserPreferences
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
