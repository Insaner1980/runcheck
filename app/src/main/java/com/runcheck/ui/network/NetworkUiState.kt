package com.runcheck.ui.network

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.NetworkReadingData
import com.runcheck.ui.common.UiText

sealed interface NetworkUiState {
    data object Loading : NetworkUiState

    @Immutable
    data class Success(
        val networkState: NetworkState,
        val signalHistory: List<NetworkReadingData> = emptyList(),
        val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val historyLoadError: UiText? = null,
        val isPro: Boolean = false
    ) : NetworkUiState

    data class Error(val message: UiText) : NetworkUiState
}

sealed interface SpeedTestPhase {
    data object Idle : SpeedTestPhase
    data object Ping : SpeedTestPhase
    data object Download : SpeedTestPhase
    data object Upload : SpeedTestPhase
    data object Completed : SpeedTestPhase
    data class Failed(val error: UiText) : SpeedTestPhase
}

@Immutable
data class SpeedTestUiState(
    val phase: SpeedTestPhase = SpeedTestPhase.Idle,
    val isRunning: Boolean = false,
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val downloadProgress: Float = 0f,
    val uploadProgress: Float = 0f,
    val historyLoadError: UiText? = null,
    val lastResult: SpeedTestResult? = null,
    val recentResults: List<SpeedTestResult> = emptyList()
)
