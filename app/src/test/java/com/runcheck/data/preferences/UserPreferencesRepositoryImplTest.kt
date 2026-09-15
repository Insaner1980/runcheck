package com.runcheck.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.runcheck.domain.model.AlertThresholds
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesRepositoryImplTest {
    @Test
    fun `constructor and empty store preserve all product defaults`() =
        runTest {
            val expected =
                UserPreferences(
                    monitoringInterval = MonitoringInterval.THIRTY,
                    notificationsEnabled = true,
                    dataRetention = DataRetention.THREE_MONTHS,
                    notifLowBattery = true,
                    notifHighTemp = true,
                    notifLowStorage = true,
                    notifChargeComplete = false,
                    temperatureUnit = TemperatureUnit.CELSIUS,
                    liveNotificationEnabled = false,
                    liveNotifCurrent = true,
                    liveNotifDrainRate = true,
                    liveNotifTemperature = true,
                    liveNotifScreenStats = false,
                    liveNotifRemainingTime = false,
                    showInfoCards = true,
                    alertBatteryThreshold = 20,
                    alertTempThreshold = 42,
                    alertStorageThreshold = 90,
                )

            assertEquals(expected, UserPreferences())
            assertEquals(expected, read(emptyPreferences()))
            assertEquals(AlertThresholds.DEFAULT_BATTERY_PERCENT, expected.alertBatteryThreshold)
            assertEquals(AlertThresholds.DEFAULT_TEMPERATURE_C, expected.alertTempThreshold)
            assertEquals(AlertThresholds.DEFAULT_STORAGE_PERCENT, expected.alertStorageThreshold)
        }

    @Test
    fun `explicit values override every ordinary default including both boolean directions`() =
        runTest {
            assertEquals(overrides, read(storedOverrides()))
        }

    @Test
    fun `each missing key uses its canonical field while preserving all other stored values`() =
        runTest {
            val defaults = UserPreferences()
            val cases =
                listOf(
                    stringPreferencesKey("monitoring_interval") to
                        overrides.copy(monitoringInterval = defaults.monitoringInterval),
                    booleanPreferencesKey("notifications") to
                        overrides.copy(notificationsEnabled = defaults.notificationsEnabled),
                    stringPreferencesKey("data_retention") to overrides.copy(dataRetention = defaults.dataRetention),
                    booleanPreferencesKey("notif_low_battery") to
                        overrides.copy(notifLowBattery = defaults.notifLowBattery),
                    booleanPreferencesKey("notif_high_temp") to overrides.copy(notifHighTemp = defaults.notifHighTemp),
                    booleanPreferencesKey("notif_low_storage") to
                        overrides.copy(notifLowStorage = defaults.notifLowStorage),
                    booleanPreferencesKey("notif_charge_complete") to
                        overrides.copy(notifChargeComplete = defaults.notifChargeComplete),
                    stringPreferencesKey("temp_unit") to overrides.copy(temperatureUnit = defaults.temperatureUnit),
                    booleanPreferencesKey("live_notif_enabled") to
                        overrides.copy(liveNotificationEnabled = defaults.liveNotificationEnabled),
                    booleanPreferencesKey("live_notif_current") to
                        overrides.copy(liveNotifCurrent = defaults.liveNotifCurrent),
                    booleanPreferencesKey("live_notif_drain_rate") to
                        overrides.copy(liveNotifDrainRate = defaults.liveNotifDrainRate),
                    booleanPreferencesKey("live_notif_temperature") to
                        overrides.copy(liveNotifTemperature = defaults.liveNotifTemperature),
                    booleanPreferencesKey("live_notif_screen_stats") to
                        overrides.copy(liveNotifScreenStats = defaults.liveNotifScreenStats),
                    booleanPreferencesKey("live_notif_remaining_time") to
                        overrides.copy(liveNotifRemainingTime = defaults.liveNotifRemainingTime),
                    booleanPreferencesKey("show_info_cards") to overrides.copy(showInfoCards = defaults.showInfoCards),
                )

            for ((key, expected) in cases) {
                val stored = storedOverrides().toMutablePreferences()
                stored.remove(key)
                assertEquals(key.name, expected, read(stored))
            }
        }

    @Test
    fun `unknown empty and case mismatched enums retain ordinary default fallback`() =
        runTest {
            val defaults = UserPreferences()
            val cases =
                listOf(
                    Triple(
                        "monitoring_interval",
                        "thirty",
                        overrides.copy(monitoringInterval = defaults.monitoringInterval),
                    ),
                    Triple("data_retention", "three_months", overrides.copy(dataRetention = defaults.dataRetention)),
                    Triple("temp_unit", "celsius", overrides.copy(temperatureUnit = defaults.temperatureUnit)),
                )

            for ((key, lowercase, expected) in cases) {
                for (invalid in listOf("UNKNOWN", "", lowercase, " $lowercase ")) {
                    val stored = storedOverrides().toMutablePreferences()
                    stored[stringPreferencesKey(key)] = invalid
                    assertEquals("$key=$invalid", expected, read(stored))
                }
            }
        }

    @Test
    fun `all valid enum selections are retained`() =
        runTest {
            for (interval in MonitoringInterval.entries) {
                assertEquals(
                    UserPreferences(monitoringInterval = interval),
                    read(preferencesOf(stringPreferencesKey("monitoring_interval") to interval.name)),
                )
            }
            for (retention in DataRetention.entries) {
                assertEquals(
                    UserPreferences(dataRetention = retention),
                    read(preferencesOf(stringPreferencesKey("data_retention") to retention.name)),
                )
            }
            for (unit in TemperatureUnit.entries) {
                assertEquals(
                    UserPreferences(temperatureUnit = unit),
                    read(preferencesOf(stringPreferencesKey("temp_unit") to unit.name)),
                )
            }
        }

    @Test
    fun `thresholds preserve valid values and clamp to existing bounds`() =
        runTest {
            for (
            values in
            listOf(
                listOf(Int.MIN_VALUE, 5, 35, 70),
                listOf(Int.MAX_VALUE, 50, 50, 99),
            )
            ) {
                val stored = values[0]
                val battery = values[1]
                val temperature = values[2]
                val storage = values[3]
                assertEquals(
                    UserPreferences(
                        alertBatteryThreshold = battery,
                        alertTempThreshold = temperature,
                        alertStorageThreshold = storage,
                    ),
                    read(
                        preferencesOf(
                            intPreferencesKey("alert_battery_threshold") to stored,
                            intPreferencesKey("alert_temp_threshold") to stored,
                            intPreferencesKey("alert_storage_threshold") to stored,
                        ),
                    ),
                )
            }
            assertEquals(
                UserPreferences(alertBatteryThreshold = 15, alertTempThreshold = 39, alertStorageThreshold = 85),
                read(
                    preferencesOf(
                        intPreferencesKey("alert_battery_threshold") to 15,
                        intPreferencesKey("alert_temp_threshold") to 39,
                        intPreferencesKey("alert_storage_threshold") to 85,
                    ),
                ),
            )
        }

    private suspend fun read(stored: Preferences): UserPreferences =
        UserPreferencesRepositoryImpl(MemoryStore(stored)).getPreferences().first()

    private val overrides =
        UserPreferences(
            monitoringInterval = MonitoringInterval.SIXTY,
            notificationsEnabled = false,
            dataRetention = DataRetention.FOREVER,
            notifLowBattery = false,
            notifHighTemp = false,
            notifLowStorage = false,
            notifChargeComplete = true,
            temperatureUnit = TemperatureUnit.FAHRENHEIT,
            liveNotificationEnabled = true,
            liveNotifCurrent = false,
            liveNotifDrainRate = false,
            liveNotifTemperature = false,
            liveNotifScreenStats = true,
            liveNotifRemainingTime = true,
            showInfoCards = false,
        )

    private fun storedOverrides(): Preferences =
        preferencesOf(
            stringPreferencesKey("monitoring_interval") to MonitoringInterval.SIXTY.name,
            booleanPreferencesKey("notifications") to false,
            stringPreferencesKey("data_retention") to DataRetention.FOREVER.name,
            booleanPreferencesKey("notif_low_battery") to false,
            booleanPreferencesKey("notif_high_temp") to false,
            booleanPreferencesKey("notif_low_storage") to false,
            booleanPreferencesKey("notif_charge_complete") to true,
            stringPreferencesKey("temp_unit") to TemperatureUnit.FAHRENHEIT.name,
            booleanPreferencesKey("live_notif_enabled") to true,
            booleanPreferencesKey("live_notif_current") to false,
            booleanPreferencesKey("live_notif_drain_rate") to false,
            booleanPreferencesKey("live_notif_temperature") to false,
            booleanPreferencesKey("live_notif_screen_stats") to true,
            booleanPreferencesKey("live_notif_remaining_time") to true,
            booleanPreferencesKey("show_info_cards") to false,
        )

    private class MemoryStore(
        initial: Preferences,
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }
}
