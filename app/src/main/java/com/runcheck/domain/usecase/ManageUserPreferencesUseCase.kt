package com.runcheck.domain.usecase

import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageUserPreferencesUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    fun observePreferences(): Flow<UserPreferences> =
        userPreferencesRepository.getPreferences()

    fun observeDismissedInfoCards(): Flow<Set<String>> =
        userPreferencesRepository.getDismissedInfoCards()

    suspend fun dismissInfoCard(id: String) {
        userPreferencesRepository.dismissInfoCard(id)
    }

    fun observeSelectedChargerId(): Flow<Long?> =
        userPreferencesRepository.observeSelectedChargerId()

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
}
