package com.devicepulse.ui.charger

import androidx.compose.runtime.Immutable
import com.devicepulse.domain.model.ChargerSummary
import com.devicepulse.domain.model.ChargingSession

sealed interface ChargerUiState {
    data object Loading : ChargerUiState
    data object Locked : ChargerUiState

    @Immutable
    data class Success(
        val chargers: List<ChargerSummary>,
        val sessions: List<ChargingSession>
    ) : ChargerUiState

    data class Error(val message: String) : ChargerUiState
}
