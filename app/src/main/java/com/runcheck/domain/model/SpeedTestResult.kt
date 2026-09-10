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
) {
    val measuredDownloadMbps: MeasuredValue<Double>
        get() = MeasuredValue(downloadMbps, confidenceFor(downloadMbps > 0.0))
    val measuredUploadMbps: MeasuredValue<Double>
        get() = MeasuredValue(uploadMbps, confidenceFor(uploadMbps > 0.0))
    val measuredPingMs: MeasuredValue<Int>
        get() = MeasuredValue(pingMs, confidenceFor(pingMs > 0))
    val measuredJitterMs: MeasuredValue<Int>?
        get() = jitterMs?.let { MeasuredValue(it, Confidence.HIGH) }

    private fun confidenceFor(available: Boolean): Confidence =
        if (available) Confidence.HIGH else Confidence.UNAVAILABLE
}
