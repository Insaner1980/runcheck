package com.devicepulse.ui.dashboard

import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Success(
        val healthScore: HealthScore,
        val batteryState: BatteryState,
        val networkState: NetworkState,
        val thermalState: ThermalState,
        val storageState: StorageState
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}
