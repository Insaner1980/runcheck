package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThermalStateUseCase @Inject constructor(
    private val thermalRepository: ThermalRepository
) {
    operator fun invoke(): Flow<ThermalState> = thermalRepository.getThermalState()
}
