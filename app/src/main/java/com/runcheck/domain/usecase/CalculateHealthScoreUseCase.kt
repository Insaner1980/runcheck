package com.runcheck.domain.usecase

import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.scoring.HealthScoreCalculator
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
