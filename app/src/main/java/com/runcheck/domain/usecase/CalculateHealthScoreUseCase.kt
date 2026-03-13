package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.scoring.HealthScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class CalculateHealthScoreUseCase @Inject constructor(
    private val getBatteryState: GetBatteryStateUseCase,
    private val getNetworkState: GetNetworkStateUseCase,
    private val getThermalState: GetThermalStateUseCase,
    private val getStorageState: GetStorageStateUseCase,
    private val calculator: HealthScoreCalculator
) {
    operator fun invoke(): Flow<HealthScore> = combine(
        getBatteryState(),
        getNetworkState(),
        getThermalState(),
        getStorageState()
    ) { battery, network, thermal, storage ->
        calculator.calculate(battery, network, thermal, storage)
    }
}
