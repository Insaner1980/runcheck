package com.runcheck.domain.model

data class NetworkReading(
    val timestamp: Long,
    val type: String,
    val signalDbm: Int?,
    val wifiSpeedMbps: Int?,
    val wifiFrequency: Int?,
    val carrier: String?,
    val networkSubtype: String?,
    val latencyMs: Int?
)
