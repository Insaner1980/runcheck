package com.devicepulse.ui.appusage

import com.devicepulse.domain.model.AppBatteryUsage

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState
    data object Locked : AppUsageUiState
    data class Success(
        val apps: List<AppBatteryUsage>
    ) : AppUsageUiState
    data class Error(val message: String) : AppUsageUiState
}
