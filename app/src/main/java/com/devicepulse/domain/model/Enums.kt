package com.devicepulse.domain.model

enum class ChargingStatus {
    CHARGING,
    DISCHARGING,
    FULL,
    NOT_CHARGING
}

enum class PlugType {
    AC,
    USB,
    WIRELESS,
    NONE
}

enum class BatteryHealth {
    GOOD,
    OVERHEAT,
    DEAD,
    OVER_VOLTAGE,
    COLD,
    UNKNOWN
}

enum class ConnectionType {
    WIFI,
    CELLULAR,
    NONE
}

enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    NO_SIGNAL
}

enum class ThermalStatus {
    NONE,
    LIGHT,
    MODERATE,
    SEVERE,
    CRITICAL,
    EMERGENCY,
    SHUTDOWN
}

enum class HealthStatus {
    HEALTHY,
    FAIR,
    POOR,
    CRITICAL
}

enum class Confidence {
    HIGH,
    LOW,
    UNAVAILABLE
}

enum class CurrentUnit {
    MICROAMPS,
    MILLIAMPS
}

enum class SignConvention {
    POSITIVE_CHARGING,
    NEGATIVE_CHARGING
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class MonitoringInterval(val minutes: Int) {
    FIFTEEN(15),
    THIRTY(30),
    SIXTY(60)
}
