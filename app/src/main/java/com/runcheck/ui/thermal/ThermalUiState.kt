package com.runcheck.ui.thermal

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.ui.common.UiText

sealed interface ThermalUiState {
    data object Loading : ThermalUiState

    data class Success(
        val thermalState: ThermalState,
        val throttlingEvents: List<ThrottlingEvent> = emptyList(),
        val isPro: Boolean = false,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        val sessionMinTemp: Float? = null,
        val sessionMaxTemp: Float? = null,
        val dismissedInfoCards: Set<String> = emptySet(),
        val showInfoCards: Boolean = true,
        val liveTempC: List<Float> = emptyList(),
        val liveHeadroom: List<Float> = emptyList(),
        val thermalHistory: List<ThermalReading> = emptyList(),
        val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val historyLoadError: UiText? = null,
    ) : ThermalUiState

    data class Error(
        val message: String,
    ) : ThermalUiState
}
