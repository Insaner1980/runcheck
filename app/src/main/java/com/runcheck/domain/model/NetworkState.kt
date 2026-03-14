package com.runcheck.domain.model

data class NetworkState(
    val connectionType: ConnectionType,
    val signalDbm: Int?,
    val signalQuality: SignalQuality,
    val wifiSsid: String? = null,
    val wifiSpeedMbps: Int? = null,
    val wifiFrequencyMhz: Int? = null,
    val carrier: String? = null,
    val networkSubtype: String? = null,
    val latencyMs: Int? = null
)
