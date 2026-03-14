package com.runcheck.ui.home

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState

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
