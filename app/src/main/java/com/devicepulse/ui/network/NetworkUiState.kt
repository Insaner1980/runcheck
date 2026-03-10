package com.devicepulse.ui.network

import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.SpeedTestResult

sealed interface NetworkUiState {
    data object Loading : NetworkUiState
    data class Success(val networkState: NetworkState) : NetworkUiState
    data class Error(val message: String) : NetworkUiState
}

sealed interface SpeedTestPhase {
    data object Idle : SpeedTestPhase
    data object Ping : SpeedTestPhase
    data object Download : SpeedTestPhase
    data object Upload : SpeedTestPhase
    data object Completed : SpeedTestPhase
    data class Failed(val error: String) : SpeedTestPhase
}

data class SpeedTestUiState(
    val phase: SpeedTestPhase = SpeedTestPhase.Idle,
    val isRunning: Boolean = false,
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val downloadProgress: Float = 0f,
    val uploadProgress: Float = 0f,
    val lastResult: SpeedTestResult? = null,
    val recentResults: List<SpeedTestResult> = emptyList()
)
