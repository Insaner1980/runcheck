package com.runcheck.domain.model

sealed interface SpeedTestProgress {
    data class CellularConfirmationRequired(
        val connectionInfo: SpeedTestConnectionInfo,
    ) : SpeedTestProgress

    data class PingPhase(
        val pingMs: Int,
        val jitterMs: Int?,
    ) : SpeedTestProgress

    data class DownloadPhase(
        val currentMbps: Double,
        val progress: Float,
    ) : SpeedTestProgress

    data class UploadPhase(
        val currentMbps: Double,
        val progress: Float,
    ) : SpeedTestProgress

    data class Completed(
        val downloadMbps: Double,
        val uploadMbps: Double,
        val pingMs: Int,
        val jitterMs: Int?,
        val serverName: String,
        val serverLocation: String?,
        val connectionInfo: SpeedTestConnectionInfo,
    ) : SpeedTestProgress

    data class Failed(
        val error: String,
    ) : SpeedTestProgress
}
