package com.runcheck.ui.thermal

import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent

sealed interface ThermalUiState {
    data object Loading : ThermalUiState
    data class Success(
        val thermalState: ThermalState,
        val throttlingEvents: List<ThrottlingEvent> = emptyList(),
        val isPro: Boolean = false,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        val sessionMinTemp: Float? = null,
        val sessionMaxTemp: Float? = null,
        val dismissedInfoCards: Set<String> = emptySet()
    ) : ThermalUiState
    data class Error(val message: String) : ThermalUiState
}
