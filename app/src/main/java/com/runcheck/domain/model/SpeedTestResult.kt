package com.runcheck.domain.model

data class SpeedTestResult(
    val id: Long = 0,
    val timestamp: Long,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Int,
    val jitterMs: Int?,
    val serverName: String?,
    val serverLocation: String?,
    val connectionType: ConnectionType,
    val networkSubtype: String?,
    val signalDbm: Int?,
)
