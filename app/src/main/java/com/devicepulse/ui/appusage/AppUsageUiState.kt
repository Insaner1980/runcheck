package com.devicepulse.ui.appusage

import com.devicepulse.data.db.entity.AppBatteryUsageEntity

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState
    data class Success(
        val apps: List<AppBatteryUsageEntity>
    ) : AppUsageUiState
    data class Error(val message: String) : AppUsageUiState
}
