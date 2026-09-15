package com.runcheck.ui.network

import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.ui.common.UiText

sealed interface NetworkUiState {
    data object Loading : NetworkUiState

    data class Success(
        val networkState: NetworkState,
        val signalHistory: List<NetworkReading> = emptyList(),
        val selectedHistoryPeriod: HistoryPeriod = HistoryPeriod.DAY,
        val historyLoadError: UiText? = null,
        val isPro: Boolean = false,
        val dismissedInfoCards: Set<String> = emptySet(),
        val showInfoCards: Boolean = true,
        val liveSignalDbm: List<Float> = emptyList(),
    ) : NetworkUiState

    data class Error(
        val message: UiText,
    ) : NetworkUiState
}

sealed interface SpeedTestPhase {
    val isRunning: Boolean
        get() =
            when (this) {
                Ping, Download, Upload -> true
                Idle, Completed, is Failed -> false
            }

    data object Idle : SpeedTestPhase

    data object Ping : SpeedTestPhase

    data object Download : SpeedTestPhase

    data object Upload : SpeedTestPhase

    data object Completed : SpeedTestPhase

    data class Failed(
        val error: UiText,
    ) : SpeedTestPhase
}

data class SpeedTestUiState(
    val phase: SpeedTestPhase = SpeedTestPhase.Idle,
    val pingMs: Int = 0,
    val jitterMs: Int? = null,
    val downloadMbps: Double = 0.0,
    val uploadMbps: Double = 0.0,
    val downloadProgress: Float = 0f,
    val uploadProgress: Float = 0f,
    val historyLoadError: UiText? = null,
    val lastResult: SpeedTestResult? = null,
    val recentResults: List<SpeedTestResult> = emptyList(),
    val showCellularWarning: Boolean = false,
) {
    val isRunning: Boolean
        get() = phase.isRunning

    val measuredPingMs: MeasuredValue<Int>
        get() = measuredNetworkValue(pingMs, pingMs > 0)
    val measuredJitterMs: MeasuredValue<Int>?
        get() = jitterMs?.let { measuredNetworkValue(it, true) }
    val measuredDownloadMbps: MeasuredValue<Double>
        get() = measuredNetworkValue(downloadMbps, downloadMbps > 0.0)
    val measuredUploadMbps: MeasuredValue<Double>
        get() = measuredNetworkValue(uploadMbps, uploadMbps > 0.0)
}

private fun <T> measuredNetworkValue(
    value: T,
    available: Boolean,
): MeasuredValue<T> =
    MeasuredValue(
        value = value,
        confidence = if (available) Confidence.HIGH else Confidence.UNAVAILABLE,
    )
