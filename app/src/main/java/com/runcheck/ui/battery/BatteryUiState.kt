package com.runcheck.ui.battery

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HistoryPeriod

sealed interface BatteryUiState {
    data object Loading : BatteryUiState

    @Immutable
    data class Success(
        val batteryState: BatteryState,
        val history: List<BatteryReading> = emptyList(),
        val selectedPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val isPro: Boolean = false
    ) : BatteryUiState

    data class Error(val message: String) : BatteryUiState
}
