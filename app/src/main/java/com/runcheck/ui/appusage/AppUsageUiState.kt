package com.runcheck.ui.appusage

import com.runcheck.ui.common.UiText

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState

    data object Locked : AppUsageUiState

    data class Success(
        val totalForegroundTimeMs: Long,
        val maxForegroundTimeMs: Long,
    ) : AppUsageUiState

    data class Error(
        val message: UiText,
    ) : AppUsageUiState
}
