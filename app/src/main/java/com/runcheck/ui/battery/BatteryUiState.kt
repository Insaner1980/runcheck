package com.runcheck.ui.battery

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ScreenUsageStats
import com.runcheck.domain.model.SleepAnalysis
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.usecase.BatteryStatistics

data class CurrentStats(
    val avg: Int,
    val min: Int,
    val max: Int,
    val sampleCount: Int
)

sealed interface BatteryUiState {
    data object Loading : BatteryUiState

    data class Success(
        val batteryState: BatteryState,
        val history: List<BatteryReading> = emptyList(),
        val selectedPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val isPro: Boolean = false,
        val currentStats: CurrentStats? = null,
        val screenUsage: ScreenUsageStats? = null,
        val sleepAnalysis: SleepAnalysis? = null,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        val statistics: BatteryStatistics? = null,
        val dismissedInfoCards: Set<String> = emptySet()
    ) : BatteryUiState

    data class Error(val message: String) : BatteryUiState
}
