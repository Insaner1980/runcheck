package com.runcheck.ui.appusage

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.AppBatteryUsage

sealed interface AppUsageUiState {
    data object Loading : AppUsageUiState
    data object Locked : AppUsageUiState
    @Immutable
    data class Success(
        val apps: List<AppBatteryUsage>
    ) : AppUsageUiState
    data class Error(val message: String) : AppUsageUiState
}
