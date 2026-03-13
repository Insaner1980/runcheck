package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.ChargerRepository
import javax.inject.Inject

class DeleteChargerUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {
    suspend operator fun invoke(id: Long) {
        chargerRepository.deleteChargerById(id)
    }
}
