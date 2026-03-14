package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThermalStateUseCase @Inject constructor(
    private val thermalRepository: ThermalRepository
) {
    operator fun invoke(): Flow<ThermalState> = thermalRepository.getThermalState()
}
