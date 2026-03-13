package com.devicepulse.ui.battery

import androidx.compose.runtime.Immutable
import com.devicepulse.domain.model.BatteryReading
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.HistoryPeriod

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
