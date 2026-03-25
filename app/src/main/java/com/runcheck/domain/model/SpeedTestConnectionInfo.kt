package com.runcheck.domain.model

data class SpeedTestConnectionInfo(
    val connectionType: ConnectionType,
    val networkSubtype: String?,
    val signalDbm: Int?
)
