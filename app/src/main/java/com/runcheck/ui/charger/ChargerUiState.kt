package com.runcheck.ui.charger

import com.runcheck.domain.model.ChargerSummary
import com.runcheck.ui.common.UiText

sealed interface ChargerUiState {
    data object Loading : ChargerUiState

    data object Locked : ChargerUiState

    data class Success(
        val chargers: List<ChargerSummary>,
        val selectedChargerId: Long?,
    ) : ChargerUiState

    data class Error(
        val message: UiText,
    ) : ChargerUiState
}
