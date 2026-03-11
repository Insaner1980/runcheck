package com.devicepulse.ui.charger

import com.devicepulse.domain.model.ChargerSummary
import com.devicepulse.domain.model.ChargingSession

sealed interface ChargerUiState {
    data object Loading : ChargerUiState

    data class Success(
        val chargers: List<ChargerSummary>,
        val sessions: List<ChargingSession>
    ) : ChargerUiState

    data class Error(val message: String) : ChargerUiState
}
