package com.devicepulse.ui.home

import androidx.compose.runtime.Immutable
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Success(
        val healthScore: HealthScore,
        val batteryState: BatteryState,
        val networkState: NetworkState,
        val thermalState: ThermalState,
        val storageState: StorageState,
        val isPro: Boolean = false
    ) : HomeUiState

    data class Error(val message: String) : HomeUiState
}
