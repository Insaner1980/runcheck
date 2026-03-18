package com.runcheck.domain.scoring

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthScoreCalculator @Inject constructor() {

    fun calculate(
        battery: BatteryState,
        network: NetworkState,
        thermal: ThermalState,
        storage: StorageState,
        recentSpeedTest: SpeedTestResult? = null
    ): HealthScore {
        val batteryScore = calculateBatteryScore(battery)
        val networkScore = calculateNetworkScore(network, recentSpeedTest)
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
            temp < 20f -> 6
            temp < 32f -> 0
            temp < 35f -> 3
            temp < 40f -> 10
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
                pct >= 95 -> 0
                pct >= 90 -> 3
                pct >= 85 -> 7
                pct >= 80 -> 12
                pct >= 75 -> 18
                pct >= 70 -> 24
                pct >= 60 -> 35
                pct >= 50 -> 45
                else -> 60
            }
        }

        return score.coerceIn(0, 100)
    }

    private fun calculateNetworkScore(
        network: NetworkState,
        recentSpeedTest: SpeedTestResult? = null
    ): Int {
        if (network.connectionType == ConnectionType.NONE) return 0

        val now = System.currentTimeMillis()
        // Check if speed test is recent (< 1 hour)
        val hasRecentSpeedTest = recentSpeedTest != null &&
            recentSpeedTest.timestamp in 0..now &&
            (now - recentSpeedTest.timestamp) < SPEED_TEST_MAX_AGE_MS

        // With speed test: signal 40%, latency 30%, download 20%, stability 10%
        // Without speed test: signal + latency only (re-weighted)
        return if (hasRecentSpeedTest) {
            calculateNetworkScoreWithSpeedTest(network, recentSpeedTest)
        } else {
            calculateNetworkScoreBasic(network)
        }
    }

    private fun calculateNetworkScoreBasic(network: NetworkState): Int {
        var score = 100

        // Signal quality impact — uses Android's own level (matches status bar)
        score -= when (network.signalQuality) {
            SignalQuality.EXCELLENT -> 0
            SignalQuality.GOOD -> 5
            SignalQuality.FAIR -> 15
            SignalQuality.POOR -> 35
            SignalQuality.NO_SIGNAL -> 70
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

    private fun calculateNetworkScoreWithSpeedTest(
        network: NetworkState,
        speedTest: SpeedTestResult
    ): Int {
        // Signal quality: 40% weight
        val signalScore = 100 - when (network.signalQuality) {
            SignalQuality.EXCELLENT -> 0
            SignalQuality.GOOD -> 10
            SignalQuality.FAIR -> 30
            SignalQuality.POOR -> 60
            SignalQuality.NO_SIGNAL -> 100
        }

        // Latency (ping): 30% weight
        val latencyScore = 100 - when {
            speedTest.pingMs < 30 -> 0
            speedTest.pingMs < 50 -> 5
            speedTest.pingMs < 100 -> 15
            speedTest.pingMs < 200 -> 30
            speedTest.pingMs < 500 -> 50
            else -> 70
        }

        // Download speed: 20% weight (relative to connection type expectations)
        val expectedDownload = when (network.connectionType) {
            ConnectionType.WIFI -> 50.0 // 50 Mbps considered good for WiFi
            ConnectionType.CELLULAR -> 20.0 // 20 Mbps considered good for cellular
            ConnectionType.NONE -> 1.0
        }
        val downloadRatio = (speedTest.downloadMbps / expectedDownload).coerceAtMost(1.0)
        val downloadScore = (downloadRatio * 100).toInt()

        // Connection stability: 10% weight (approximated from jitter)
        val stabilityScore = when {
            speedTest.jitterMs == null -> 80
            speedTest.jitterMs < 5 -> 100
            speedTest.jitterMs < 15 -> 85
            speedTest.jitterMs < 30 -> 65
            speedTest.jitterMs < 50 -> 45
            else -> 20
        }

        val weightedScore = (
            signalScore * 0.40f +
            latencyScore * 0.30f +
            downloadScore * 0.20f +
            stabilityScore * 0.10f
        ).toInt()

        return weightedScore.coerceIn(0, 100)
    }

    private fun calculateThermalScore(thermal: ThermalState): Int {
        var score = 100

        // Battery temperature impact (ideal: 20-35°C)
        val temp = thermal.batteryTempC
        score -= when {
            temp < 10 -> 20
            temp < 20f -> 8
            temp < 30f -> 0
            temp < 33f -> 2
            temp < 35f -> 5
            temp < 40f -> 15
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
        } ?: run {
            score -= 2
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
            usagePct < 10 -> 4
            usagePct < 25 -> 2
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
        private const val BATTERY_WEIGHT = 0.40f
        private const val NETWORK_WEIGHT = 0.25f
        private const val THERMAL_WEIGHT = 0.25f
        private const val STORAGE_WEIGHT = 0.10f
        private const val SPEED_TEST_MAX_AGE_MS = 3_600_000L // 1 hour
    }
}
