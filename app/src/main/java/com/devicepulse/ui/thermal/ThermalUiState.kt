package com.devicepulse.ui.thermal

import com.devicepulse.data.db.entity.ThrottlingEventEntity
import com.devicepulse.domain.model.ThermalState

sealed interface ThermalUiState {
    data object Loading : ThermalUiState
    data class Success(
        val thermalState: ThermalState,
        val throttlingEvents: List<ThrottlingEventEntity> = emptyList(),
        val isPro: Boolean = false
    ) : ThermalUiState
    data class Error(val message: String) : ThermalUiState
}
