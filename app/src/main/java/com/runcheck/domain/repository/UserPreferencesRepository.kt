package com.runcheck.domain.repository

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun setMonitoringInterval(interval: MonitoringInterval)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setDataRetention(retention: DataRetention)
    suspend fun setCrashReportingEnabled(enabled: Boolean)
    fun getPermissionEducationSeen(): Flow<Boolean>
    suspend fun setPermissionEducationSeen(seen: Boolean)
    suspend fun getAppUsageLastCollectedAt(): Long?
    suspend fun setAppUsageLastCollectedAt(timestamp: Long)
    fun observeSelectedChargerId(): Flow<Long?>
    suspend fun getSelectedChargerId(): Long?
    suspend fun setSelectedChargerId(chargerId: Long?)
    // New settings
    suspend fun setNotifLowBattery(enabled: Boolean)
    suspend fun setNotifHighTemp(enabled: Boolean)
    suspend fun setNotifLowStorage(enabled: Boolean)
    suspend fun setNotifChargeComplete(enabled: Boolean)
    suspend fun setAlertBatteryThreshold(value: Int)
    suspend fun setAlertTempThreshold(value: Int)
    suspend fun setAlertStorageThreshold(value: Int)
    suspend fun setTemperatureUnit(unit: TemperatureUnit)
}
