package com.runcheck.ui.home

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalState
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStatus
import com.runcheck.ui.common.UiText

internal enum class HomeProCardState {
    TRIAL,
    EXPIRED_TRIAL,
    PRO,
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val healthScore: HealthScore,
        val batteryState: BatteryState,
        val networkState: NetworkState,
        val thermalState: ThermalState,
        val storageState: StorageState,
        val measurementTimestampMillis: Long,
        val insights: List<Insight> = emptyList(),
        val totalInsightCount: Int = 0,
        val unseenInsightCount: Int = 0,
        val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
        val proState: ProState = ProState(),
        val monitoringStale: Boolean = false,
        val showWelcomeSheet: Boolean = false,
        val showDay5Banner: Boolean = false,
        val showExpirationModal: Boolean = false,
        val showUpgradeCard: Boolean = false,
    ) : HomeUiState {
        val isPro: Boolean get() = proState.isPro
        internal val proCardState: HomeProCardState?
            get() = resolveHomeProCardState(proState.status, showUpgradeCard)
    }

    data class Error(
        val message: UiText,
    ) : HomeUiState
}

internal fun resolveHomeProCardState(
    proStatus: ProStatus,
    showUpgradeCard: Boolean,
): HomeProCardState? =
    when {
        proStatus == ProStatus.TRIAL_ACTIVE -> HomeProCardState.TRIAL
        proStatus == ProStatus.TRIAL_EXPIRED && showUpgradeCard -> HomeProCardState.EXPIRED_TRIAL
        proStatus == ProStatus.PRO_PURCHASED -> HomeProCardState.PRO
        else -> null
    }
