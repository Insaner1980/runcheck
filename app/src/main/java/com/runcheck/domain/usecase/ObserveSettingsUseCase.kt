package com.runcheck.domain.usecase

import com.runcheck.domain.model.DeviceProfileInfo
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.DeviceProfileRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class ObserveSettingsUseCase
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val deviceProfileRepository: DeviceProfileRepository,
    ) {
        operator fun invoke(): Flow<SettingsData> =
            combine(
                userPreferencesRepository.getPreferences(),
                deviceProfileRepository.getProfile(),
            ) { preferences, deviceProfile ->
                SettingsData(
                    preferences = preferences,
                    deviceProfile = deviceProfile,
                )
            }
    }

data class SettingsData(
    val preferences: UserPreferences,
    val deviceProfile: DeviceProfileInfo?,
)
