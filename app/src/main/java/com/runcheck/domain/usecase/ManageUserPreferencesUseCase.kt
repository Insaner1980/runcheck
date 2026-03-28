package com.runcheck.domain.usecase

import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageUserPreferencesUseCase
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
    ) {
        fun observePreferences(): Flow<UserPreferences> = userPreferencesRepository.getPreferences()

        fun observeSelectedChargerId(): Flow<Long?> = userPreferencesRepository.observeSelectedChargerId()

        suspend fun setSelectedChargerId(chargerId: Long?) {
            userPreferencesRepository.setSelectedChargerId(chargerId)
        }

        suspend fun setNotifLowBattery(enabled: Boolean) {
            userPreferencesRepository.setNotifLowBattery(enabled)
        }

        suspend fun setNotifHighTemp(enabled: Boolean) {
            userPreferencesRepository.setNotifHighTemp(enabled)
        }

        suspend fun setNotifLowStorage(enabled: Boolean) {
            userPreferencesRepository.setNotifLowStorage(enabled)
        }

        suspend fun setNotifChargeComplete(enabled: Boolean) {
            userPreferencesRepository.setNotifChargeComplete(enabled)
        }

        suspend fun setAlertBatteryThreshold(value: Int) {
            userPreferencesRepository.setAlertBatteryThreshold(value)
        }

        suspend fun setAlertTempThreshold(value: Int) {
            userPreferencesRepository.setAlertTempThreshold(value)
        }

        suspend fun setAlertStorageThreshold(value: Int) {
            userPreferencesRepository.setAlertStorageThreshold(value)
        }

        suspend fun setTemperatureUnit(unit: TemperatureUnit) {
            userPreferencesRepository.setTemperatureUnit(unit)
        }

        suspend fun setLiveNotificationEnabled(enabled: Boolean) {
            userPreferencesRepository.setLiveNotificationEnabled(enabled)
        }

        suspend fun setLiveNotifCurrent(enabled: Boolean) {
            userPreferencesRepository.setLiveNotifCurrent(enabled)
        }

        suspend fun setLiveNotifDrainRate(enabled: Boolean) {
            userPreferencesRepository.setLiveNotifDrainRate(enabled)
        }

        suspend fun setLiveNotifTemperature(enabled: Boolean) {
            userPreferencesRepository.setLiveNotifTemperature(enabled)
        }

        suspend fun setLiveNotifScreenStats(enabled: Boolean) {
            userPreferencesRepository.setLiveNotifScreenStats(enabled)
        }

        suspend fun setLiveNotifRemainingTime(enabled: Boolean) {
            userPreferencesRepository.setLiveNotifRemainingTime(enabled)
        }

        suspend fun setShowInfoCards(enabled: Boolean) {
            userPreferencesRepository.setShowInfoCards(enabled)
        }

        suspend fun resetAlertThresholds() {
            val defaults = UserPreferences()
            userPreferencesRepository.setAlertBatteryThreshold(defaults.alertBatteryThreshold)
            userPreferencesRepository.setAlertTempThreshold(defaults.alertTempThreshold)
            userPreferencesRepository.setAlertStorageThreshold(defaults.alertStorageThreshold)
        }
    }
