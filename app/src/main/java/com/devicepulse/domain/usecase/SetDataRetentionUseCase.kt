package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.DataRetention
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetDataRetentionUseCase @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val proStatusProvider: ProStatusProvider
) {
    suspend operator fun invoke(retention: DataRetention) {
        if (!proStatusProvider.isPro()) return
        userPreferencesRepository.setDataRetention(retention)
    }
}
