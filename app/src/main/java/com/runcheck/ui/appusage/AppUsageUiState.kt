package com.runcheck.ui.appusage

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState
    data object Locked : AppUsageUiState
    data class Success(
        val totalForegroundTimeMs: Long,
        val maxForegroundTimeMs: Long
    ) : AppUsageUiState
    data class Error(val message: String) : AppUsageUiState
}
