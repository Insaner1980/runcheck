package com.devicepulse.ui.thermal

import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThrottlingEvent

sealed interface ThermalUiState {
    data object Loading : ThermalUiState
    data class Success(
        val thermalState: ThermalState,
        val throttlingEvents: List<ThrottlingEvent> = emptyList(),
        val isPro: Boolean = false
    ) : ThermalUiState
    data class Error(val message: String) : ThermalUiState
}
