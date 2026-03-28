package com.runcheck.service.monitor

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.usecase.MonitoringAlertSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.monitoringAlertStateDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "monitoring_alert_state")

@Singleton
class MonitoringAlertStateStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun getLastSnapshot(): MonitoringAlertSnapshot? {
            val preferences = context.monitoringAlertStateDataStore.data.first()
            val batteryLevel = preferences[KEY_LAST_BATTERY_LEVEL] ?: return null
            val batteryTempC = preferences[KEY_LAST_BATTERY_TEMP_C] ?: return null
            val storageUsagePercent = preferences[KEY_LAST_STORAGE_USAGE_PERCENT] ?: return null
            val chargingStatus =
                preferences[KEY_LAST_CHARGING_STATUS]
                    ?.let(::parseChargingStatus)
                    ?: return null

            return MonitoringAlertSnapshot(
                batteryLevel = batteryLevel,
                batteryTempC = batteryTempC,
                storageUsagePercent = storageUsagePercent,
                chargingStatus = chargingStatus,
            )
        }

        suspend fun wasChargeCompleteFired(): Boolean {
            val preferences = context.monitoringAlertStateDataStore.data.first()
            return preferences[KEY_CHARGE_COMPLETE_FIRED] ?: false
        }

        suspend fun update(
            snapshot: MonitoringAlertSnapshot,
            chargeCompleteFired: Boolean? = null,
        ) {
            context.monitoringAlertStateDataStore.edit { preferences ->
                preferences[KEY_LAST_BATTERY_LEVEL] = snapshot.batteryLevel
                preferences[KEY_LAST_BATTERY_TEMP_C] = snapshot.batteryTempC
                preferences[KEY_LAST_STORAGE_USAGE_PERCENT] = snapshot.storageUsagePercent
                preferences[KEY_LAST_CHARGING_STATUS] = snapshot.chargingStatus.name
                if (chargeCompleteFired != null) {
                    preferences[KEY_CHARGE_COMPLETE_FIRED] = chargeCompleteFired
                }
            }
        }

        private fun parseChargingStatus(name: String): ChargingStatus? =
            runCatching { enumValueOf<ChargingStatus>(name) }.getOrNull()

        private companion object {
            private val KEY_LAST_BATTERY_LEVEL = intPreferencesKey("last_battery_level")
            private val KEY_LAST_BATTERY_TEMP_C = floatPreferencesKey("last_battery_temp_c")
            private val KEY_LAST_STORAGE_USAGE_PERCENT = floatPreferencesKey("last_storage_usage_percent")
            private val KEY_LAST_CHARGING_STATUS = stringPreferencesKey("last_charging_status")
            private val KEY_CHARGE_COMPLETE_FIRED = booleanPreferencesKey("charge_complete_fired")
        }
    }
