package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.repository.BatteryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBatteryStateUseCase
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
    ) {
        operator fun invoke(): Flow<BatteryState> = batteryRepository.getBatteryState()
    }
