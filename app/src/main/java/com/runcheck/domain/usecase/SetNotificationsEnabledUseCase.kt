package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetNotificationsEnabledUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(enabled: Boolean) {
        userPreferencesRepository.setNotificationsEnabled(enabled)
    }
}
