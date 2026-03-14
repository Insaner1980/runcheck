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
    val latencyMs: Int? = null,
    val estimatedDownstreamKbps: Int? = null,
    val estimatedUpstreamKbps: Int? = null,
    val isMetered: Boolean? = null,
    val isRoaming: Boolean? = null,
    val isVpn: Boolean? = null,
    val ipAddresses: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val mtuBytes: Int? = null,
    val wifiBssid: String? = null,
    val wifiStandard: String? = null
)
