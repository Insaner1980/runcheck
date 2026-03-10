package com.devicepulse.domain.scoring

import com.devicepulse.domain.model.BatteryHealth
import com.devicepulse.domain.model.BatteryState
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.HealthScore
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.StorageState
import com.devicepulse.domain.model.ThermalState
import com.devicepulse.domain.model.ThermalStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthScoreCalculator @Inject constructor() {

    fun calculate(
        battery: BatteryState,
        network: NetworkState,
        thermal: ThermalState,
        storage: StorageState
    ): HealthScore {
        val batteryScore = calculateBatteryScore(battery)
        val networkScore = calculateNetworkScore(network)
        val thermalScore = calculateThermalScore(thermal)
        val storageScore = calculateStorageScore(storage)

        val overallScore = (
            batteryScore * BATTERY_WEIGHT +
            networkScore * NETWORK_WEIGHT +
            thermalScore * THERMAL_WEIGHT +
            storageScore * STORAGE_WEIGHT
        ).toInt().coerceIn(0, 100)

        return HealthScore(
            overallScore = overallScore,
            batteryScore = batteryScore,
            networkScore = networkScore,
            thermalScore = thermalScore,
            storageScore = storageScore,
            status = HealthScore.statusFromScore(overallScore)
        )
    }

    private fun calculateBatteryScore(battery: BatteryState): Int {
        var score = 100

        // Health impact
        when (battery.health) {
            BatteryHealth.GOOD -> { /* no deduction */ }
            BatteryHealth.OVERHEAT -> score -= 40
            BatteryHealth.DEAD -> score -= 80
            BatteryHealth.OVER_VOLTAGE -> score -= 50
            BatteryHealth.COLD -> score -= 20
            BatteryHealth.UNKNOWN -> score -= 10
        }

        // Temperature impact (ideal: 20-35°C)
        val temp = battery.temperatureC
        score -= when {
            temp < 0 -> 30
            temp < 10 -> 15
            temp in 10f..35f -> 0
            temp in 35f..40f -> 10
            temp in 40f..45f -> 25
            else -> 40
        }

        // Voltage stability (ideal: 3700-4200 mV)
        val voltage = battery.voltageMv
        score -= when {
            voltage < 3200 -> 20
            voltage < 3500 -> 10
            voltage in 3500..4250 -> 0
            else -> 15
        }

        // Health percent if available
        battery.healthPercent?.let { pct ->
            score -= when {
                pct >= 80 -> 0
                pct >= 60 -> 15
                pct >= 40 -> 30
                else -> 50
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateNetworkScore(network: NetworkState): Int {
        if (network.connectionType == ConnectionType.NONE) return 0

        var score = 100

        // Signal strength impact (null = unavailable, minor deduction)
        val dbm = network.signalDbm
        score -= if (dbm == null) {
            5 // Unknown signal — connected, so assume OK
        } else when {
            dbm >= -50 -> 0
            dbm >= -60 -> 5
            dbm >= -70 -> 15
            dbm >= -80 -> 30
            dbm >= -90 -> 50
            dbm >= -100 -> 70
            else -> 90
        }

        // Latency impact
        network.latencyMs?.let { latency ->
            score -= when {
                latency < 50 -> 0
                latency < 100 -> 5
                latency < 200 -> 10
                latency < 500 -> 20
                latency < 1000 -> 35
                else -> 50
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateThermalScore(thermal: ThermalState): Int {
        var score = 100

        // Battery temperature impact (ideal: 20-35°C)
        val temp = thermal.batteryTempC
        score -= when {
            temp < 10 -> 20
            temp in 10f..35f -> 0
            temp in 35f..40f -> 15
            temp in 40f..45f -> 35
            else -> 60
        }

        // CPU temperature impact
        thermal.cpuTempC?.let { cpuTemp ->
            score -= when {
                cpuTemp < 50 -> 0
                cpuTemp < 60 -> 5
                cpuTemp < 70 -> 15
                cpuTemp < 80 -> 30
                else -> 50
            }
        }

        // Thermal status impact
        score -= when (thermal.thermalStatus) {
            ThermalStatus.NONE -> 0
            ThermalStatus.LIGHT -> 10
            ThermalStatus.MODERATE -> 25
            ThermalStatus.SEVERE -> 45
            ThermalStatus.CRITICAL -> 65
            ThermalStatus.EMERGENCY -> 80
            ThermalStatus.SHUTDOWN -> 95
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateStorageScore(storage: StorageState): Int {
        var score = 100

        // Usage percentage impact
        val usagePct = storage.usagePercent
        score -= when {
            usagePct < 50 -> 0
            usagePct < 70 -> 5
            usagePct < 80 -> 15
            usagePct < 90 -> 35
            usagePct < 95 -> 55
            else -> 80
        }

        return score.coerceIn(0, 100)
    }

    companion object {
        private const val BATTERY_WEIGHT = 0.35f
        private const val NETWORK_WEIGHT = 0.20f
        private const val THERMAL_WEIGHT = 0.25f
        private const val STORAGE_WEIGHT = 0.20f
    }
}
