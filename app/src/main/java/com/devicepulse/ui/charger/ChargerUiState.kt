package com.devicepulse.ui.charger

import com.devicepulse.data.db.entity.ChargingSessionEntity
import com.devicepulse.domain.model.ChargerSummary

sealed interface ChargerUiState {
    data object Loading : ChargerUiState

    data class Success(
        val chargers: List<ChargerSummary>,
        val sessions: List<ChargingSessionEntity>
    ) : ChargerUiState

    data class Error(val message: String) : ChargerUiState
}
