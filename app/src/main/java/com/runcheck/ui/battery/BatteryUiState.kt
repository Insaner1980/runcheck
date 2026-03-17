package com.runcheck.ui.battery

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.usecase.BatteryStatistics
import com.runcheck.service.monitor.ScreenUsageStats
import com.runcheck.service.monitor.SleepAnalysis

data class CurrentStats(
    val avg: Int,
    val min: Int,
    val max: Int,
    val sampleCount: Int
)

sealed interface BatteryUiState {
    data object Loading : BatteryUiState

    @Immutable
    data class Success(
        val batteryState: BatteryState,
        val history: List<BatteryReading> = emptyList(),
        val selectedPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val isPro: Boolean = false,
        val currentStats: CurrentStats? = null,
        val screenUsage: ScreenUsageStats? = null,
        val sleepAnalysis: SleepAnalysis? = null,
        val statistics: BatteryStatistics? = null
    ) : BatteryUiState

    data class Error(val message: String) : BatteryUiState
}
