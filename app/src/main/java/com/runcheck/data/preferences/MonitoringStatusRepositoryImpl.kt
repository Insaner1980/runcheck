package com.runcheck.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.monitoringStatusDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "monitoring_status")

@Singleton
class MonitoringStatusRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : MonitoringStatusRepository {
        override fun observeLastWorkerHeartbeat(): Flow<MonitoringHeartbeat?> =
            context.monitoringStatusDataStore.data
                .catch { error ->
                    if (error is IOException) {
                        emit(emptyPreferences())
                    } else {
                        throw error
                    }
                }.map { prefs ->
                    val recordedAtEpochMillis = prefs[KEY_LAST_WORKER_HEARTBEAT_AT]
                    val recordedAtUptimeMillis = prefs[KEY_LAST_WORKER_HEARTBEAT_UPTIME]
                    val intervalMinutes = prefs[KEY_LAST_WORKER_HEARTBEAT_INTERVAL]
                    if (recordedAtEpochMillis != null && recordedAtUptimeMillis != null && intervalMinutes != null) {
                        MonitoringHeartbeat(
                            recordedAtEpochMillis = recordedAtEpochMillis,
                            recordedAtUptimeMillis = recordedAtUptimeMillis,
                            intervalMinutes = intervalMinutes,
                        )
                    } else {
                        null
                    }
                }

        override suspend fun setLastWorkerHeartbeat(heartbeat: MonitoringHeartbeat) {
            context.monitoringStatusDataStore.edit { prefs ->
                prefs[KEY_LAST_WORKER_HEARTBEAT_AT] = heartbeat.recordedAtEpochMillis
                prefs[KEY_LAST_WORKER_HEARTBEAT_UPTIME] = heartbeat.recordedAtUptimeMillis
                prefs[KEY_LAST_WORKER_HEARTBEAT_INTERVAL] = heartbeat.intervalMinutes
            }
        }

        override suspend fun clearLastWorkerHeartbeat() {
            context.monitoringStatusDataStore.edit { prefs ->
                prefs.clear()
            }
        }

        private companion object {
            val KEY_LAST_WORKER_HEARTBEAT_AT = longPreferencesKey("last_worker_heartbeat_at")
            val KEY_LAST_WORKER_HEARTBEAT_UPTIME = longPreferencesKey("last_worker_heartbeat_uptime")
            val KEY_LAST_WORKER_HEARTBEAT_INTERVAL = intPreferencesKey("last_worker_heartbeat_interval")
        }
    }
