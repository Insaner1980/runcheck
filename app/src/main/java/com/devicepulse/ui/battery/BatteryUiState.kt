package com.devicepulse.ui.battery

import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.domain.model.BatteryState

sealed interface BatteryUiState {
    data object Loading : BatteryUiState

    data class Success(
        val batteryState: BatteryState,
        val history: List<BatteryReadingEntity> = emptyList()
    ) : BatteryUiState

    data class Error(val message: String) : BatteryUiState
}
