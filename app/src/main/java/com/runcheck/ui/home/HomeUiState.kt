package com.runcheck.ui.home

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.pro.ProState

sealed interface HomeUiState {
    data object Loading : HomeUiState

    @Immutable
    data class Success(
        val healthScore: HealthScore,
        val batteryState: BatteryState,
        val networkState: NetworkState,
        val thermalState: ThermalState,
        val storageState: StorageState,
        val proState: ProState = ProState(),
        val showWelcomeSheet: Boolean = false,
        val showDay5Banner: Boolean = false,
        val showExpirationModal: Boolean = false,
        val showUpgradeCard: Boolean = false
    ) : HomeUiState {
        val isPro: Boolean get() = proState.isPro
    }

    data class Error(val message: String) : HomeUiState
}
