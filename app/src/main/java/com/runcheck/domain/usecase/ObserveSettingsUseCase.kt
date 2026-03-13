package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.model.UserPreferences
import com.devicepulse.domain.repository.DeviceProfileRepository
import com.devicepulse.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveSettingsUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val deviceProfileRepository: DeviceProfileRepository
) {
    operator fun invoke(): Flow<SettingsData> = combine(
        userPreferencesRepository.getPreferences(),
        deviceProfileRepository.getProfile()
    ) { preferences, deviceProfile ->
        SettingsData(
            preferences = preferences,
            deviceProfile = deviceProfile
        )
    }
}

data class SettingsData(
    val preferences: UserPreferences,
    val deviceProfile: DeviceProfileInfo?
)
