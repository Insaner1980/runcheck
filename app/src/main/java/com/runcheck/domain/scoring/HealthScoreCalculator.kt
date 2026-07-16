package com.runcheck.domain.scoring

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryScoreDiagnostics
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthScoreDiagnostics
import com.runcheck.domain.model.NetworkScoreDiagnostics
import com.runcheck.domain.model.NetworkScoreMode
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageScoreDiagnostics
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalScoreDiagnostics
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthScoreCalculator
    @Inject
    constructor() {
        fun calculate(
            battery: BatteryState,
            network: NetworkState,
            thermal: ThermalState,
            storage: StorageState,
        ): HealthScore =
            calculate(
                battery = battery,
                network = network,
                thermal = thermal,
                storage = storage,
                recentSpeedTest = null,
                nowMillis = 0L,
            )

        fun calculate(
            battery: BatteryState,
            network: NetworkState,
            thermal: ThermalState,
            storage: StorageState,
            recentSpeedTest: SpeedTestResult?,
            nowMillis: Long,
        ): HealthScore {
            val batteryResult = calculateBatteryScore(battery)
            val networkResult = calculateNetworkScore(network, recentSpeedTest, nowMillis)
            val thermalResult = calculateThermalScore(thermal)
            val storageResult = calculateStorageScoreResult(storage)

            val overallScore =
                roundedWeightedAverage(
                    batteryResult.score * BATTERY_WEIGHT +
                        networkResult.score * NETWORK_WEIGHT +
                        thermalResult.score * THERMAL_WEIGHT +
                        storageResult.score * STORAGE_WEIGHT,
                )

            return HealthScore(
                overallScore = overallScore,
                batteryScore = batteryResult.score,
                networkScore = networkResult.score,
                thermalScore = thermalResult.score,
                storageScore = storageResult.score,
                status = HealthScore.statusFromScore(overallScore),
                diagnostics =
                    HealthScoreDiagnostics(
                        battery = batteryResult.diagnostics,
                        network = networkResult.diagnostics,
                        thermal = thermalResult.diagnostics,
                        storage = storageResult.diagnostics,
                    ),
            )
        }

        private fun calculateBatteryScore(battery: BatteryState): BatteryScoreResult {
            val healthPenalty = batteryHealthPenalty(battery.health)
            val temperaturePenalty = batteryTemperaturePenalty(battery.temperatureC)
            val voltagePenalty = batteryVoltagePenalty(battery.voltageMv)
            val capacityPenalty = batteryCapacityPenalty(battery.healthPercent)
            val score =
                100 -
                    healthPenalty -
                    temperaturePenalty -
                    voltagePenalty -
                    capacityPenalty
            return BatteryScoreResult(
                score = score.coerceIn(0, 100),
                diagnostics =
                    BatteryScoreDiagnostics(
                        healthPenalty = healthPenalty,
                        temperaturePenalty = temperaturePenalty,
                        voltagePenalty = voltagePenalty,
                        capacityPenalty = capacityPenalty,
                    ),
            )
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
            nowMillis: Long,
        ): NetworkScoreResult {
            if (network.connectionType == ConnectionType.NONE) {
                return NetworkScoreResult(
                    score = 0,
                    diagnostics =
                        NetworkScoreDiagnostics(
                            mode = NetworkScoreMode.DISCONNECTED,
                            signalScore = 0,
                            liveLatencyPenalty = null,
                            speedTestPingScore = null,
                            speedTestDownloadScore = null,
                            speedTestJitterScore = null,
                            speedTestAgeMillis = null,
                            speedTestWeightPercent = 0,
                        ),
                )
            }

            val basicResult = calculateNetworkScoreBasic(network)
            val speedTest = recentSpeedTest ?: return basicResult
            if (speedTest.connectionType != network.connectionType) return basicResult
            val speedTestAgeMs = nowMillis - speedTest.timestamp
            if (speedTestAgeMs !in 0 until SPEED_TEST_MAX_AGE_MS) return basicResult

            val speedTestResult = calculateNetworkScoreWithSpeedTest(network, speedTest, speedTestAgeMs)
            if (speedTestAgeMs <= SPEED_TEST_FADE_START_MS) return speedTestResult

            // Ease the expiring measurement into the live-only score so the strict
            // one-hour cutoff cannot cause a visible step change at the boundary.
            val remainingMillis = SPEED_TEST_MAX_AGE_MS - speedTestAgeMs
            val blendedScore =
                divideRounded(
                    basicResult.score * SPEED_TEST_FADE_DURATION_MS +
                        (speedTestResult.score - basicResult.score) * remainingMillis,
                    SPEED_TEST_FADE_DURATION_MS,
                ).coerceIn(0, 100).toInt()
            return speedTestResult.copy(
                score = blendedScore,
                diagnostics =
                    speedTestResult.diagnostics.copy(
                        mode = NetworkScoreMode.FADING_SPEED_TEST,
                        speedTestWeightPercent =
                            divideRounded(remainingMillis * 100L, SPEED_TEST_FADE_DURATION_MS).toInt(),
                    ),
            )
        }

        private fun calculateNetworkScoreBasic(network: NetworkState): NetworkScoreResult {
            val signalPenalty =
                when (network.signalQuality) {
                    SignalQuality.EXCELLENT -> 0
                    SignalQuality.GOOD -> 5
                    SignalQuality.FAIR -> 15
                    SignalQuality.POOR -> 35
                    SignalQuality.NO_SIGNAL -> 70
                }

            val latencyPenalty =
                network.latencyMs?.let { latency ->
                    when {
                        latency < 50 -> 0
                        latency < 100 -> 5
                        latency < 200 -> 10
                        latency < 500 -> 20
                        latency < 1000 -> 35
                        else -> 50
                    }
                }

            return NetworkScoreResult(
                score = (100 - signalPenalty - (latencyPenalty ?: 0)).coerceIn(0, 100),
                diagnostics =
                    NetworkScoreDiagnostics(
                        mode = NetworkScoreMode.LIVE,
                        signalScore = 100 - signalPenalty,
                        liveLatencyPenalty = latencyPenalty,
                        speedTestPingScore = null,
                        speedTestDownloadScore = null,
                        speedTestJitterScore = null,
                        speedTestAgeMillis = null,
                        speedTestWeightPercent = 0,
                    ),
            )
        }

        private fun calculateNetworkScoreWithSpeedTest(
            network: NetworkState,
            speedTest: SpeedTestResult,
            speedTestAgeMs: Long,
        ): NetworkScoreResult {
            val signalScore = signalQualityScore(network.signalQuality)
            val pingScore = latencyPingScore(speedTest.pingMs)
            val downloadScore = downloadSpeedScore(speedTest.downloadMbps, network.connectionType)
            val jitterScore = speedTest.jitterMs?.let(::jitterStabilityScore)
            val scoreComponents =
                buildList {
                    add(signalScore to 40)
                    add(pingScore to 30)
                    add(downloadScore to 20)
                    jitterScore?.let { score ->
                        add(score to 10)
                    }
                }
            val totalWeight = scoreComponents.sumOf { it.second }
            val weightedScore = scoreComponents.sumOf { (score, weight) -> score * weight } / totalWeight

            return NetworkScoreResult(
                score = weightedScore.coerceIn(0, 100),
                diagnostics =
                    NetworkScoreDiagnostics(
                        mode = NetworkScoreMode.SPEED_TEST,
                        signalScore = signalScore,
                        liveLatencyPenalty = null,
                        speedTestPingScore = pingScore,
                        speedTestDownloadScore = downloadScore,
                        speedTestJitterScore = jitterScore,
                        speedTestAgeMillis = speedTestAgeMs,
                        speedTestWeightPercent = 100,
                    ),
            )
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

        private fun jitterStabilityScore(jitterMs: Int): Int =
            when {
                jitterMs < 5 -> 100
                jitterMs < 15 -> 85
                jitterMs < 30 -> 65
                jitterMs < 50 -> 45
                else -> 20
            }

        private fun calculateThermalScore(thermal: ThermalState): ThermalScoreResult {
            val batteryTemperaturePenalty = thermalBatteryTempPenalty(thermal.batteryTempC)
            val cpuTemperaturePenalty = thermalCpuTempPenalty(thermal.cpuTempC)
            val statusPenalty = thermalStatusPenalty(thermal.thermalStatus)
            val score =
                100 -
                    batteryTemperaturePenalty -
                    cpuTemperaturePenalty -
                    statusPenalty
            return ThermalScoreResult(
                score = score.coerceIn(0, 100),
                diagnostics =
                    ThermalScoreDiagnostics(
                        batteryTemperaturePenalty = batteryTemperaturePenalty,
                        cpuTemperaturePenalty = cpuTemperaturePenalty,
                        statusPenalty = statusPenalty,
                    ),
            )
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
            if (cpuTempC == null) return 0
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

        fun calculateStorageScore(storage: StorageState): Int = calculateStorageScoreResult(storage).score

        private fun calculateStorageScoreResult(storage: StorageState): StorageScoreResult {
            val usagePenalty =
                when {
                    storage.usagePercent < 10 -> 4
                    storage.usagePercent < 25 -> 2
                    storage.usagePercent < 50 -> 0
                    storage.usagePercent < 70 -> 5
                    storage.usagePercent < 75 -> 15
                    storage.usagePercent < 85 -> 35
                    storage.usagePercent < 95 -> 55
                    else -> 80
                }

            return StorageScoreResult(
                score = (100 - usagePenalty).coerceIn(0, 100),
                diagnostics = StorageScoreDiagnostics(usagePenalty = usagePenalty),
            )
        }

        private fun roundedWeightedAverage(weightedScore: Int): Int =
            ((weightedScore + TOTAL_WEIGHT / 2) / TOTAL_WEIGHT).coerceIn(0, 100)

        private fun divideRounded(
            numerator: Long,
            denominator: Long,
        ): Long = (numerator + denominator / 2) / denominator

        private data class BatteryScoreResult(
            val score: Int,
            val diagnostics: BatteryScoreDiagnostics,
        )

        private data class NetworkScoreResult(
            val score: Int,
            val diagnostics: NetworkScoreDiagnostics,
        )

        private data class ThermalScoreResult(
            val score: Int,
            val diagnostics: ThermalScoreDiagnostics,
        )

        private data class StorageScoreResult(
            val score: Int,
            val diagnostics: StorageScoreDiagnostics,
        )

        companion object {
            private const val BATTERY_WEIGHT = 40
            private const val NETWORK_WEIGHT = 25
            private const val THERMAL_WEIGHT = 25
            private const val STORAGE_WEIGHT = 10
            private const val TOTAL_WEIGHT = 100
            private const val SPEED_TEST_MAX_AGE_MS = 3_600_000L // 1 hour
            private const val SPEED_TEST_FADE_DURATION_MS = 300_000L // 5 minutes
            private const val SPEED_TEST_FADE_START_MS = SPEED_TEST_MAX_AGE_MS - SPEED_TEST_FADE_DURATION_MS
        }
    }
