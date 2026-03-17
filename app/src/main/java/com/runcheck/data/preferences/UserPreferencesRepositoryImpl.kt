package com.runcheck.data.preferences

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : com.runcheck.domain.repository.UserPreferencesRepository {

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    override fun getPreferences(): Flow<UserPreferences> = preferencesFlow.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME_MODE]
                ?.let { stored -> enumValueOrNull<ThemeMode>(stored) }
                ?: ThemeMode.SYSTEM,
            amoledBlack = prefs[KEY_AMOLED_BLACK] ?: false,
            dynamicColors = prefs[KEY_DYNAMIC_COLORS] ?: true,
            monitoringInterval = prefs[KEY_MONITORING_INTERVAL]
                ?.let { stored -> enumValueOrNull<MonitoringInterval>(stored) }
                ?: MonitoringInterval.THIRTY,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            dataRetention = prefs[KEY_DATA_RETENTION]
                ?.let { stored -> enumValueOrNull<DataRetention>(stored) }
                ?: DataRetention.THREE_MONTHS,
            crashReportingEnabled = prefs[KEY_CRASH_REPORTING] ?: false,
            notifLowBattery = prefs[KEY_NOTIF_LOW_BATTERY] ?: true,
            notifHighTemp = prefs[KEY_NOTIF_HIGH_TEMP] ?: true,
            notifLowStorage = prefs[KEY_NOTIF_LOW_STORAGE] ?: true,
            notifChargeComplete = prefs[KEY_NOTIF_CHARGE_COMPLETE] ?: false,
            alertBatteryThreshold = prefs[KEY_ALERT_BATTERY] ?: 20,
            alertTempThreshold = prefs[KEY_ALERT_TEMP] ?: 42,
            alertStorageThreshold = prefs[KEY_ALERT_STORAGE] ?: 90,
            temperatureUnit = prefs[KEY_TEMP_UNIT]
                ?.let { stored -> enumValueOrNull<TemperatureUnit>(stored) }
                ?: TemperatureUnit.CELSIUS
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    override suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    override suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLORS] = enabled }
    }

    override suspend fun setMonitoringInterval(interval: MonitoringInterval) {
        context.dataStore.edit { it[KEY_MONITORING_INTERVAL] = interval.name }
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    override suspend fun setDataRetention(retention: DataRetention) {
        context.dataStore.edit { it[KEY_DATA_RETENTION] = retention.name }
    }

    override suspend fun setCrashReportingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_CRASH_REPORTING] = enabled }
    }

    override fun getPermissionEducationSeen(): Flow<Boolean> = preferencesFlow.map { prefs ->
        prefs[KEY_PERMISSION_EDUCATION_SEEN] ?: false
    }

    override suspend fun setPermissionEducationSeen(seen: Boolean) {
        context.dataStore.edit { it[KEY_PERMISSION_EDUCATION_SEEN] = seen }
    }

    override suspend fun getAppUsageLastCollectedAt(): Long? =
        preferencesFlow.map { prefs -> prefs[KEY_APP_USAGE_LAST_COLLECTED_AT] }.first()

    override suspend fun setAppUsageLastCollectedAt(timestamp: Long) {
        context.dataStore.edit { it[KEY_APP_USAGE_LAST_COLLECTED_AT] = timestamp }
    }

    override suspend fun setNotifLowBattery(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_LOW_BATTERY] = enabled }
    }

    override suspend fun setNotifHighTemp(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_HIGH_TEMP] = enabled }
    }

    override suspend fun setNotifLowStorage(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_LOW_STORAGE] = enabled }
    }

    override suspend fun setNotifChargeComplete(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_CHARGE_COMPLETE] = enabled }
    }

    override suspend fun setAlertBatteryThreshold(value: Int) {
        context.dataStore.edit { it[KEY_ALERT_BATTERY] = value }
    }

    override suspend fun setAlertTempThreshold(value: Int) {
        context.dataStore.edit { it[KEY_ALERT_TEMP] = value }
    }

    override suspend fun setAlertStorageThreshold(value: Int) {
        context.dataStore.edit { it[KEY_ALERT_STORAGE] = value }
    }

    override suspend fun setTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { it[KEY_TEMP_UNIT] = unit.name }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        private val KEY_DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val KEY_MONITORING_INTERVAL = stringPreferencesKey("monitoring_interval")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_DATA_RETENTION = stringPreferencesKey("data_retention")
        private val KEY_CRASH_REPORTING = booleanPreferencesKey("crash_reporting_enabled")
        private val KEY_PERMISSION_EDUCATION_SEEN = booleanPreferencesKey("permission_education_seen")
        private val KEY_APP_USAGE_LAST_COLLECTED_AT = longPreferencesKey("app_usage_last_collected_at")
        private val KEY_NOTIF_LOW_BATTERY = booleanPreferencesKey("notif_low_battery")
        private val KEY_NOTIF_HIGH_TEMP = booleanPreferencesKey("notif_high_temp")
        private val KEY_NOTIF_LOW_STORAGE = booleanPreferencesKey("notif_low_storage")
        private val KEY_NOTIF_CHARGE_COMPLETE = booleanPreferencesKey("notif_charge_complete")
        private val KEY_ALERT_BATTERY = intPreferencesKey("alert_battery_threshold")
        private val KEY_ALERT_TEMP = intPreferencesKey("alert_temp_threshold")
        private val KEY_ALERT_STORAGE = intPreferencesKey("alert_storage_threshold")
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    runCatching { enumValueOf<T>(name) }.getOrNull()
