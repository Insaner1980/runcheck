package com.devicepulse.domain.model

sealed interface SpeedTestProgress {
    data class PingPhase(val pingMs: Int, val jitterMs: Int) : SpeedTestProgress
    data class DownloadPhase(val currentMbps: Double, val progress: Float) : SpeedTestProgress
    data class UploadPhase(val currentMbps: Double, val progress: Float) : SpeedTestProgress
    data class Completed(
        val downloadMbps: Double,
        val uploadMbps: Double,
        val pingMs: Int,
        val jitterMs: Int,
        val serverName: String,
        val serverLocation: String
    ) : SpeedTestProgress
    data class Failed(val error: String) : SpeedTestProgress
}
