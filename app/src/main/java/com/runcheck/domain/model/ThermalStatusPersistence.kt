package com.runcheck.domain.model

/** Stable identifiers compatible with historical thermal readings and throttling events. */
object ThermalStatusPersistence {
    fun toCode(status: ThermalStatus): Int =
        when (status) {
            ThermalStatus.NONE -> 0
            ThermalStatus.LIGHT -> 1
            ThermalStatus.MODERATE -> 2
            ThermalStatus.SEVERE -> 3
            ThermalStatus.CRITICAL -> 4
            ThermalStatus.EMERGENCY -> 5
            ThermalStatus.SHUTDOWN -> 6
        }

    fun fromCode(code: Int): ThermalStatus? =
        when (code) {
            0 -> ThermalStatus.NONE
            1 -> ThermalStatus.LIGHT
            2 -> ThermalStatus.MODERATE
            3 -> ThermalStatus.SEVERE
            4 -> ThermalStatus.CRITICAL
            5 -> ThermalStatus.EMERGENCY
            6 -> ThermalStatus.SHUTDOWN
            else -> null
        }

    fun toId(status: ThermalStatus): String =
        when (status) {
            ThermalStatus.NONE -> "NONE"
            ThermalStatus.LIGHT -> "LIGHT"
            ThermalStatus.MODERATE -> "MODERATE"
            ThermalStatus.SEVERE -> "SEVERE"
            ThermalStatus.CRITICAL -> "CRITICAL"
            ThermalStatus.EMERGENCY -> "EMERGENCY"
            ThermalStatus.SHUTDOWN -> "SHUTDOWN"
        }

    fun fromId(id: String): ThermalStatus? =
        when (id) {
            "NONE" -> ThermalStatus.NONE
            "LIGHT" -> ThermalStatus.LIGHT
            "MODERATE" -> ThermalStatus.MODERATE
            "SEVERE" -> ThermalStatus.SEVERE
            "CRITICAL" -> ThermalStatus.CRITICAL
            "EMERGENCY" -> ThermalStatus.EMERGENCY
            "SHUTDOWN" -> ThermalStatus.SHUTDOWN
            else -> null
        }
}
