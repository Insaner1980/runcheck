package com.runcheck.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
class MonitoringStatusRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : MonitoringStatusRepository {

    override fun observeLastWorkerHeartbeatAt(): Flow<Long?> {
        return context.monitoringStatusDataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs -> prefs[KEY_LAST_WORKER_HEARTBEAT_AT] }
    }

    override suspend fun setLastWorkerHeartbeatAt(timestamp: Long) {
        context.monitoringStatusDataStore.edit { prefs ->
            prefs[KEY_LAST_WORKER_HEARTBEAT_AT] = timestamp
        }
    }

    private companion object {
        val KEY_LAST_WORKER_HEARTBEAT_AT = longPreferencesKey("last_worker_heartbeat_at")
    }
}
