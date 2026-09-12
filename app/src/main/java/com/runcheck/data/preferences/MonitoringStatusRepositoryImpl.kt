package com.runcheck.data.preferences

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.runcheck.domain.model.MonitoringHeartbeat
import com.runcheck.domain.repository.MonitoringStatusRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.monitoringStatusDataStore: DataStore<Preferences>
    by preferencesDataStore(
        name = "monitoring_status",
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    )

@Singleton
class MonitoringStatusRepositoryImpl
    internal constructor(
        private val dataStore: DataStore<Preferences>,
        private val bootCount: Int = -1,
    ) : MonitoringStatusRepository {
        @Inject
        constructor(
            @ApplicationContext context: Context,
        ) : this(
            context.monitoringStatusDataStore,
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1),
        )

        override fun observeLastWorkerHeartbeat(): Flow<MonitoringHeartbeat?> =
            dataStore.data
                .map { prefs ->
                    val recordedAtEpochMillis = prefs[KEY_LAST_WORKER_HEARTBEAT_AT]
                    val recordedAtUptimeMillis = prefs[KEY_LAST_WORKER_HEARTBEAT_UPTIME]
                    val intervalMinutes = prefs[KEY_LAST_WORKER_HEARTBEAT_INTERVAL]
                    if (recordedAtEpochMillis != null && recordedAtUptimeMillis != null && intervalMinutes != null) {
                        MonitoringHeartbeat(
                            recordedAtEpochMillis = recordedAtEpochMillis,
                            recordedAtUptimeMillis = recordedAtUptimeMillis,
                            intervalMinutes = intervalMinutes,
                            isFromPreviousBoot =
                                prefs[KEY_LAST_WORKER_HEARTBEAT_BOOT_COUNT]?.let { recordedBoot ->
                                    bootCount >= 0 && recordedBoot != bootCount
                                } ?: false,
                        )
                    } else {
                        null
                    }
                }

        override suspend fun setLastWorkerHeartbeat(heartbeat: MonitoringHeartbeat) {
            dataStore.edit { prefs ->
                prefs[KEY_LAST_WORKER_HEARTBEAT_AT] = heartbeat.recordedAtEpochMillis
                prefs[KEY_LAST_WORKER_HEARTBEAT_UPTIME] = heartbeat.recordedAtUptimeMillis
                prefs[KEY_LAST_WORKER_HEARTBEAT_INTERVAL] = heartbeat.intervalMinutes
                if (bootCount >= 0) {
                    prefs[KEY_LAST_WORKER_HEARTBEAT_BOOT_COUNT] = bootCount
                } else {
                    prefs.remove(KEY_LAST_WORKER_HEARTBEAT_BOOT_COUNT)
                }
            }
        }

        override suspend fun clearLastWorkerHeartbeat() {
            dataStore.edit { prefs ->
                prefs.clear()
            }
        }

        private companion object {
            val KEY_LAST_WORKER_HEARTBEAT_AT = longPreferencesKey("last_worker_heartbeat_at")
            val KEY_LAST_WORKER_HEARTBEAT_UPTIME = longPreferencesKey("last_worker_heartbeat_uptime")
            val KEY_LAST_WORKER_HEARTBEAT_INTERVAL = intPreferencesKey("last_worker_heartbeat_interval")
            val KEY_LAST_WORKER_HEARTBEAT_BOOT_COUNT = intPreferencesKey("last_worker_heartbeat_boot_count")
        }
    }
