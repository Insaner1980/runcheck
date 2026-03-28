package com.runcheck.domain.model

enum class MonitoringInterval(
    val minutes: Int,
) {
    FIFTEEN(15),
    THIRTY(30),
    SIXTY(60),
}
