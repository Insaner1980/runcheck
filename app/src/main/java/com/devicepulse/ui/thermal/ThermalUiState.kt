package com.devicepulse.ui.thermal

import com.devicepulse.domain.model.ThermalState

sealed interface ThermalUiState {
    data object Loading : ThermalUiState
    data class Success(val thermalState: ThermalState) : ThermalUiState
    data class Error(val message: String) : ThermalUiState
}
