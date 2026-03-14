package com.runcheck.ui.thermal

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent

sealed interface ThermalUiState {
    data object Loading : ThermalUiState
    @Immutable
    data class Success(
        val thermalState: ThermalState,
        val throttlingEvents: List<ThrottlingEvent> = emptyList(),
        val isPro: Boolean = false
    ) : ThermalUiState
    data class Error(val message: String) : ThermalUiState
}
