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
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.InfoCardDismissalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : UserPreferencesRepository, InfoCardDismissalRepository {

    private val preferencesFlow: Flow<Preferences> = context.dataStore.data.catch { error ->
        if (error is IOException) {
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    // Dismissals are app-local UI state. Clearing app data or reinstalling should show cards again.
    override fun observeDismissedCardIds(): Flow<Set<String>> = preferencesFlow.transform { prefs ->
        val stored = prefs[KEY_DISMISSED_INFO_CARDS] ?: emptySet()
        val normalized = normalizeDismissedCardIds(stored)
        if (normalized != stored) {
            context.dataStore.edit { it[KEY_DISMISSED_INFO_CARDS] = normalized }
        }
        emit(normalized)
    }

    override suspend fun dismissCard(cardId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DISMISSED_INFO_CARDS] ?: emptySet()
            prefs[KEY_DISMISSED_INFO_CARDS] = normalizeDismissedCardIds(current + cardId)
        }
    }

    override suspend fun resetDismissedCards() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_DISMISSED_INFO_CARDS)
        }
    }

    override fun getPreferences(): Flow<UserPreferences> = preferencesFlow.map { prefs ->
        UserPreferences(
            monitoringInterval = prefs[KEY_MONITORING_INTERVAL]
                ?.let { stored -> enumValueOrNull<MonitoringInterval>(stored) }
                ?: MonitoringInterval.THIRTY,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
            dataRetention = prefs[KEY_DATA_RETENTION]
                ?.let { stored -> enumValueOrNull<DataRetention>(stored) }
                ?: DataRetention.THREE_MONTHS,
            notifLowBattery = prefs[KEY_NOTIF_LOW_BATTERY] ?: true,
            notifHighTemp = prefs[KEY_NOTIF_HIGH_TEMP] ?: true,
            notifLowStorage = prefs[KEY_NOTIF_LOW_STORAGE] ?: true,
            notifChargeComplete = prefs[KEY_NOTIF_CHARGE_COMPLETE] ?: false,
            alertBatteryThreshold = prefs[KEY_ALERT_BATTERY] ?: 20,
            alertTempThreshold = prefs[KEY_ALERT_TEMP] ?: 42,
            alertStorageThreshold = prefs[KEY_ALERT_STORAGE] ?: 90,
            temperatureUnit = prefs[KEY_TEMP_UNIT]
                ?.let { stored -> enumValueOrNull<TemperatureUnit>(stored) }
                ?: TemperatureUnit.CELSIUS,
            liveNotificationEnabled = prefs[KEY_LIVE_NOTIF_ENABLED] ?: false,
            liveNotifCurrent = prefs[KEY_LIVE_NOTIF_CURRENT] ?: true,
            liveNotifDrainRate = prefs[KEY_LIVE_NOTIF_DRAIN_RATE] ?: true,
            liveNotifTemperature = prefs[KEY_LIVE_NOTIF_TEMPERATURE] ?: true,
            liveNotifScreenStats = prefs[KEY_LIVE_NOTIF_SCREEN_STATS] ?: false,
            liveNotifRemainingTime = prefs[KEY_LIVE_NOTIF_REMAINING_TIME] ?: false,
            showInfoCards = prefs[KEY_SHOW_INFO_CARDS] ?: true
        )
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

    override fun observeSelectedChargerId(): Flow<Long?> = preferencesFlow.map { prefs ->
        prefs[KEY_SELECTED_CHARGER_ID]
    }

    override suspend fun getSelectedChargerId(): Long? =
        preferencesFlow.map { prefs -> prefs[KEY_SELECTED_CHARGER_ID] }.first()

    override suspend fun setSelectedChargerId(chargerId: Long?) {
        context.dataStore.edit { prefs ->
            if (chargerId == null) {
                prefs.remove(KEY_SELECTED_CHARGER_ID)
            } else {
                prefs[KEY_SELECTED_CHARGER_ID] = chargerId
            }
        }
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

    override suspend fun setLiveNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_ENABLED] = enabled }
    }

    override suspend fun setLiveNotifCurrent(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_CURRENT] = enabled }
    }

    override suspend fun setLiveNotifDrainRate(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_DRAIN_RATE] = enabled }
    }

    override suspend fun setLiveNotifTemperature(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_TEMPERATURE] = enabled }
    }

    override suspend fun setLiveNotifScreenStats(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_SCREEN_STATS] = enabled }
    }

    override suspend fun setLiveNotifRemainingTime(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LIVE_NOTIF_REMAINING_TIME] = enabled }
    }

    override suspend fun setShowInfoCards(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_INFO_CARDS] = enabled }
    }

    companion object {
        private val KEY_MONITORING_INTERVAL = stringPreferencesKey("monitoring_interval")
        private val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications")
        private val KEY_DATA_RETENTION = stringPreferencesKey("data_retention")
        private val KEY_PERMISSION_EDUCATION_SEEN = booleanPreferencesKey("permission_education_seen")
        private val KEY_APP_USAGE_LAST_COLLECTED_AT = longPreferencesKey("app_usage_last_collected_at")
        private val KEY_SELECTED_CHARGER_ID = longPreferencesKey("selected_charger_id")
        private val KEY_NOTIF_LOW_BATTERY = booleanPreferencesKey("notif_low_battery")
        private val KEY_NOTIF_HIGH_TEMP = booleanPreferencesKey("notif_high_temp")
        private val KEY_NOTIF_LOW_STORAGE = booleanPreferencesKey("notif_low_storage")
        private val KEY_NOTIF_CHARGE_COMPLETE = booleanPreferencesKey("notif_charge_complete")
        private val KEY_ALERT_BATTERY = intPreferencesKey("alert_battery_threshold")
        private val KEY_ALERT_TEMP = intPreferencesKey("alert_temp_threshold")
        private val KEY_ALERT_STORAGE = intPreferencesKey("alert_storage_threshold")
        private val KEY_TEMP_UNIT = stringPreferencesKey("temp_unit")
        private val KEY_LIVE_NOTIF_ENABLED = booleanPreferencesKey("live_notif_enabled")
        private val KEY_LIVE_NOTIF_CURRENT = booleanPreferencesKey("live_notif_current")
        private val KEY_LIVE_NOTIF_DRAIN_RATE = booleanPreferencesKey("live_notif_drain_rate")
        private val KEY_LIVE_NOTIF_TEMPERATURE = booleanPreferencesKey("live_notif_temperature")
        private val KEY_LIVE_NOTIF_SCREEN_STATS = booleanPreferencesKey("live_notif_screen_stats")
        private val KEY_LIVE_NOTIF_REMAINING_TIME = booleanPreferencesKey("live_notif_remaining_time")
        private val KEY_SHOW_INFO_CARDS = booleanPreferencesKey("show_info_cards")
        private val KEY_DISMISSED_INFO_CARDS = stringSetPreferencesKey("dismissed_info_cards")
        private val VERSIONED_CARD_ID_REGEX = Regex("^(.*)_v(\\d+)$")
    }

    private fun normalizeDismissedCardIds(rawIds: Set<String>): Set<String> {
        if (rawIds.isEmpty()) return emptySet()
        return rawIds
            .map(::parseDismissedCardId)
            .groupBy(DismissedCardId::baseKey)
            .values
            .map { variants ->
                variants.maxWithOrNull(
                    compareBy<DismissedCardId> { it.version ?: 0 }
                        .thenBy { it.raw }
                )?.canonical()
            }
            .filterNotNull()
            .toSet()
    }

    private fun parseDismissedCardId(rawId: String): DismissedCardId {
        val match = VERSIONED_CARD_ID_REGEX.matchEntire(rawId)
        val baseKey = match?.groupValues?.get(1) ?: rawId
        val version = match?.groupValues?.get(2)?.toIntOrNull()
        return DismissedCardId(raw = rawId, baseKey = baseKey, version = version)
    }

    private data class DismissedCardId(
        val raw: String,
        val baseKey: String,
        val version: Int?
    ) {
        fun canonical(): String = version?.let { "${baseKey}_v$it" } ?: raw
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
    runCatching { enumValueOf<T>(name) }.getOrNull()
