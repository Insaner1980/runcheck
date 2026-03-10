package com.devicepulse.domain.usecase

import com.devicepulse.data.battery.BatteryRepository
import com.devicepulse.domain.model.BatteryState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBatteryStateUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository
) {
    operator fun invoke(): Flow<BatteryState> = batteryRepository.getBatteryState()
}
