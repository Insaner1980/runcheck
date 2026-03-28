package com.runcheck.domain.scoring

import androidx.annotation.CheckResult
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
class HealthScoreCalculator
    @Inject
    constructor() {
        @CheckResult
        fun calculate(
            battery: BatteryState,
            network: NetworkState,
            thermal: ThermalState,
            storage: StorageState,
            recentSpeedTest: SpeedTestResult? = null,
        ): HealthScore {
            val batteryScore = calculateBatteryScore(battery)
            val networkScore = calculateNetworkScore(network, recentSpeedTest)
            val thermalScore = calculateThermalScore(thermal)
            val storageScore = calculateStorageScore(storage)

            val overallScore =
                (
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
                status = HealthScore.statusFromScore(overallScore),
            )
        }

        private fun calculateBatteryScore(battery: BatteryState): Int {
            val score =
                100 -
                    batteryHealthPenalty(battery.health) -
                    batteryTemperaturePenalty(battery.temperatureC) -
                    batteryVoltagePenalty(battery.voltageMv) -
                    batteryCapacityPenalty(battery.healthPercent)
            return score.coerceIn(0, 100)
        }

        private fun batteryHealthPenalty(health: BatteryHealth): Int =
            when (health) {
                BatteryHealth.GOOD -> 0
                BatteryHealth.OVERHEAT -> 40
                BatteryHealth.DEAD -> 80
                BatteryHealth.OVER_VOLTAGE -> 50
                BatteryHealth.COLD -> 20
                BatteryHealth.UNKNOWN -> 10
            }

        private fun batteryTemperaturePenalty(tempC: Float): Int =
            when {
                tempC < 0 -> 30
                tempC < 10 -> 15
                tempC < 20f -> 6
                tempC < 32f -> 0
                tempC < 35f -> 3
                tempC < 40f -> 10
                tempC in 40f..45f -> 25
                else -> 40
            }

        private fun batteryVoltagePenalty(voltageMv: Int): Int =
            when {
                voltageMv < 3200 -> 20
                voltageMv < 3500 -> 10
                voltageMv in 3500..4250 -> 0
                else -> 15
            }

        private fun batteryCapacityPenalty(healthPercent: Int?): Int {
            if (healthPercent == null) return 0
            return when {
                healthPercent >= 95 -> 0
                healthPercent >= 90 -> 3
                healthPercent >= 85 -> 7
                healthPercent >= 80 -> 12
                healthPercent >= 75 -> 18
                healthPercent >= 70 -> 24
                healthPercent >= 60 -> 35
                healthPercent >= 50 -> 45
                else -> 60
            }
        }

        private fun calculateNetworkScore(
            network: NetworkState,
            recentSpeedTest: SpeedTestResult? = null,
        ): Int {
            if (network.connectionType == ConnectionType.NONE) return 0

            val now = System.currentTimeMillis()
            // Check if speed test is recent (< 1 hour)
            val hasRecentSpeedTest =
                recentSpeedTest != null &&
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
            score -=
                when (network.signalQuality) {
                    SignalQuality.EXCELLENT -> 0
                    SignalQuality.GOOD -> 5
                    SignalQuality.FAIR -> 15
                    SignalQuality.POOR -> 35
                    SignalQuality.NO_SIGNAL -> 70
                }

            // Latency impact
            network.latencyMs?.let { latency ->
                score -=
                    when {
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
            speedTest: SpeedTestResult,
        ): Int {
            val signalScore = signalQualityScore(network.signalQuality)
            val latencyScore = latencyPingScore(speedTest.pingMs)
            val downloadScore = downloadSpeedScore(speedTest.downloadMbps, network.connectionType)
            val stabilityScore = jitterStabilityScore(speedTest.jitterMs)

            val weightedScore =
                (
                    signalScore * 0.40f +
                        latencyScore * 0.30f +
                        downloadScore * 0.20f +
                        stabilityScore * 0.10f
                ).toInt()

            return weightedScore.coerceIn(0, 100)
        }

        private fun signalQualityScore(quality: SignalQuality): Int =
            100 -
                when (quality) {
                    SignalQuality.EXCELLENT -> 0
                    SignalQuality.GOOD -> 10
                    SignalQuality.FAIR -> 30
                    SignalQuality.POOR -> 60
                    SignalQuality.NO_SIGNAL -> 100
                }

        private fun latencyPingScore(pingMs: Int): Int =
            100 -
                when {
                    pingMs <= 0 -> 20
                    pingMs < 30 -> 0
                    pingMs < 50 -> 5
                    pingMs < 100 -> 15
                    pingMs < 200 -> 30
                    pingMs < 500 -> 50
                    else -> 70
                }

        private fun downloadSpeedScore(
            downloadMbps: Double,
            connectionType: ConnectionType,
        ): Int {
            val expectedDownload =
                when (connectionType) {
                    ConnectionType.WIFI -> 50.0
                    ConnectionType.CELLULAR -> 20.0
                    ConnectionType.VPN -> 20.0
                    ConnectionType.NONE -> 1.0
                }
            val downloadRatio = (downloadMbps / expectedDownload).coerceAtMost(1.0)
            return (downloadRatio * 100).toInt()
        }

        private fun jitterStabilityScore(jitterMs: Int?): Int =
            when {
                jitterMs == null -> 80
                jitterMs < 5 -> 100
                jitterMs < 15 -> 85
                jitterMs < 30 -> 65
                jitterMs < 50 -> 45
                else -> 20
            }

        private fun calculateThermalScore(thermal: ThermalState): Int {
            val score =
                100 -
                    thermalBatteryTempPenalty(thermal.batteryTempC) -
                    thermalCpuTempPenalty(thermal.cpuTempC) -
                    thermalStatusPenalty(thermal.thermalStatus)
            return score.coerceIn(0, 100)
        }

        private fun thermalBatteryTempPenalty(tempC: Float): Int =
            when {
                tempC < 10 -> 20
                tempC < 20f -> 8
                tempC < 30f -> 0
                tempC < 33f -> 2
                tempC < 35f -> 5
                tempC < 40f -> 15
                tempC in 40f..45f -> 35
                else -> 60
            }

        private fun thermalCpuTempPenalty(cpuTempC: Float?): Int {
            if (cpuTempC == null) return 2
            return when {
                cpuTempC < 50 -> 0
                cpuTempC < 60 -> 5
                cpuTempC < 70 -> 15
                cpuTempC < 80 -> 30
                else -> 50
            }
        }

        private fun thermalStatusPenalty(status: ThermalStatus): Int =
            when (status) {
                ThermalStatus.NONE -> 0
                ThermalStatus.LIGHT -> 10
                ThermalStatus.MODERATE -> 25
                ThermalStatus.SEVERE -> 45
                ThermalStatus.CRITICAL -> 65
                ThermalStatus.EMERGENCY -> 80
                ThermalStatus.SHUTDOWN -> 95
            }

        private fun calculateStorageScore(storage: StorageState): Int {
            var score = 100

            // Usage percentage impact
            val usagePct = storage.usagePercent
            score -=
                when {
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
