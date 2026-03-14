package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ChargerRepository
import javax.inject.Inject

class DeleteChargerUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {
    suspend operator fun invoke(id: Long) {
        chargerRepository.deleteChargerById(id)
    }
}
