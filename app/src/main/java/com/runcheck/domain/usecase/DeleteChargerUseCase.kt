package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class DeleteChargerUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke(id: Long) {
        if (userPreferencesRepository.getSelectedChargerId() == id) {
            userPreferencesRepository.setSelectedChargerId(null)
        }
        chargerRepository.deleteChargerById(id)
    }
}
