package com.devicepulse.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getPreferences(): Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[KEY_THEME_MODE]?.let { ThemeMode.valueOf(it) }
                ?: ThemeMode.SYSTEM,
            amoledBlack = prefs[KEY_AMOLED_BLACK] ?: false,
            dynamicColors = prefs[KEY_DYNAMIC_COLORS] ?: true,
            monitoringInterval = prefs[KEY_MONITORING_INTERVAL]?.let {
                MonitoringInterval.valueOf(it)
            } ?: MonitoringInterval.THIRTY,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AMOLED_BLACK] = enabled }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC_COLORS] = enabled }
    }

    suspend fun setMonitoringInterval(interval: MonitoringInterval) {
        context.dataStore.edit { it[KEY_MONITORING_INTERVAL] = interval.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS] = enabled }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        private val KEY_DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val KEY_MONITORING_INTERVAL = stringPreferencesKey("monitoring_interval")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
    }
}
