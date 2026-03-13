package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.ChargerRepository
import javax.inject.Inject

class AddChargerUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {
    suspend operator fun invoke(name: String): Long =
        chargerRepository.insertCharger(name)
}
