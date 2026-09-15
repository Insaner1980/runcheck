package com.runcheck.domain.model

enum class ThermalStatus {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN,
    ;

    val isThrottling: Boolean
        get() = this >= SEVERE
}
