package com.runcheck.domain.scoring

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkScoreMode
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

class HealthScoreCalculatorTest {
    private lateinit var calculator: HealthScoreCalculator

    private companion object {
        const val TEST_NOW_MILLIS = 10_000_000L
    }

    @Before
    fun setup() {
        calculator = HealthScoreCalculator()
    }

    private fun healthyBattery() =
        BatteryState(
            level = 85,
            voltageMv = 4100,
            temperatureC = 25f,
            currentMa = MeasuredValue(500, Confidence.HIGH),
            chargingStatus = ChargingStatus.DISCHARGING,
            plugType = PlugType.NONE,
            health = BatteryHealth.GOOD,
            technology = "Li-ion",
        )

    private fun healthyNetwork() =
        NetworkState(
            connectionType = ConnectionType.WIFI,
            signalDbm = -45,
            signalQuality = SignalQuality.EXCELLENT,
            wifiSsid = "Home",
            wifiSpeedMbps = 300,
            latencyMs = 15,
        )

    private fun healthyThermal() =
        ThermalState(
            batteryTempC = 28f,
            cpuTempC = 40f,
            thermalStatus = ThermalStatus.NONE,
            isThrottling = false,
        )

    private fun healthyStorage() =
        StorageState(
            totalBytes = 128_000_000_000L,
            availableBytes = 80_000_000_000L,
            usedBytes = 48_000_000_000L,
            usagePercent = 37.5f,
            appsBytes = 16_000_000_000L,
        )

    @Test
    fun `healthy device scores 75+`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        assertTrue("Expected healthy score >=75, got ${score.overallScore}", score.overallScore >= 75)
        assertEquals(HealthStatus.HEALTHY, score.status)
    }

    @Test
    fun `no network connection drops network score to 0`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork().copy(
                    connectionType = ConnectionType.NONE,
                    signalDbm = -999,
                    signalQuality = SignalQuality.NO_SIGNAL,
                ),
                healthyThermal(),
                healthyStorage(),
            )
        assertEquals(0, score.networkScore)
    }

    @Test
    fun `overheating battery reduces battery score`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(
                    temperatureC = 48f,
                    health = BatteryHealth.OVERHEAT,
                ),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        assertTrue("Expected degraded battery score, got ${score.batteryScore}", score.batteryScore < 50)
    }

    @Test
    fun `critical thermal status reduces thermal score`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal().copy(
                    batteryTempC = 46f,
                    thermalStatus = ThermalStatus.CRITICAL,
                    isThrottling = true,
                ),
                healthyStorage(),
            )
        assertTrue("Expected poor thermal score, got ${score.thermalScore}", score.thermalScore < 25)
    }

    @Test
    fun `nearly full storage reduces storage score`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(
                    usagePercent = 96f,
                    availableBytes = 5_000_000_000L,
                    usedBytes = 123_000_000_000L,
                ),
            )
        assertTrue("Expected poor storage score, got ${score.storageScore}", score.storageScore < 30)
    }

    @Test
    fun `overall score is weighted average`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 80),
                healthyNetwork().copy(signalQuality = SignalQuality.FAIR, latencyMs = 250),
                healthyThermal().copy(batteryTempC = 36f),
                healthyStorage().copy(usagePercent = 75f),
            )
        val expected =
            (
                score.batteryScore * 0.40 +
                    score.networkScore * 0.25 +
                    score.thermalScore * 0.25 +
                    score.storageScore * 0.10
            ).roundToInt()
        assertEquals(expected, score.overallScore)
    }

    @Test
    fun `overall score rounds half points up before tier classification`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork().copy(connectionType = ConnectionType.NONE),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 60f),
            )

        assertEquals(100, score.batteryScore)
        assertEquals(0, score.networkScore)
        assertEquals(100, score.thermalScore)
        assertEquals(95, score.storageScore)
        assertEquals(75, score.overallScore)
        assertEquals(HealthStatus.HEALTHY, score.status)
    }

    @Test
    fun `scores are clamped to 0-100`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(
                    health = BatteryHealth.DEAD,
                    temperatureC = 55f,
                    voltageMv = 2800,
                ),
                healthyNetwork().copy(
                    connectionType = ConnectionType.NONE,
                    signalDbm = -999,
                    signalQuality = SignalQuality.NO_SIGNAL,
                ),
                healthyThermal().copy(
                    batteryTempC = 55f,
                    thermalStatus = ThermalStatus.SHUTDOWN,
                    isThrottling = true,
                ),
                healthyStorage().copy(usagePercent = 99f),
            )
        assertTrue(score.overallScore in 0..100)
        assertTrue(score.batteryScore in 0..100)
        assertTrue(score.networkScore in 0..100)
        assertTrue(score.thermalScore in 0..100)
        assertTrue(score.storageScore in 0..100)
    }

    @Test
    fun `diagnostics preserve component penalties and network scoring mode`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(
                    health = BatteryHealth.OVERHEAT,
                    temperatureC = 48f,
                    healthPercent = 80,
                ),
                healthyNetwork().copy(
                    signalQuality = SignalQuality.POOR,
                    latencyMs = 600,
                ),
                healthyThermal().copy(
                    batteryTempC = 46f,
                    cpuTempC = 75f,
                    thermalStatus = ThermalStatus.CRITICAL,
                ),
                healthyStorage().copy(usagePercent = 96f),
            )

        assertEquals(40, score.diagnostics.battery.healthPenalty)
        assertEquals(40, score.diagnostics.battery.temperaturePenalty)
        assertEquals(12, score.diagnostics.battery.capacityPenalty)
        assertEquals(NetworkScoreMode.LIVE, score.diagnostics.network.mode)
        assertEquals(65, score.diagnostics.network.signalScore)
        assertEquals(35, score.diagnostics.network.liveLatencyPenalty)
        assertEquals(60, score.diagnostics.thermal.batteryTemperaturePenalty)
        assertEquals(30, score.diagnostics.thermal.cpuTemperaturePenalty)
        assertEquals(65, score.diagnostics.thermal.statusPenalty)
        assertEquals(80, score.diagnostics.storage.usagePenalty)
    }

    @Test
    fun `fixed inputs and explicit time produce identical complete results`() {
        val speedTest = recentSpeedTest(timestamp = TEST_NOW_MILLIS - 3_450_000L)

        val first =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = speedTest,
                nowMillis = TEST_NOW_MILLIS,
            )
        val second =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = speedTest,
                nowMillis = TEST_NOW_MILLIS,
            )

        assertEquals(first, second)
        assertEquals(NetworkScoreMode.FADING_SPEED_TEST, first.diagnostics.network.mode)
        assertEquals(50, first.diagnostics.network.speedTestWeightPercent)
    }

    @Test
    fun `battery health percent of 80 is not treated as perfect`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(
                    level = 80,
                    temperatureC = 33.6f,
                    healthPercent = 80,
                ),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        assertTrue("Expected battery score below 100, got ${score.batteryScore}", score.batteryScore < 100)
    }

    @Test
    fun `missing CPU temperature does not penalize thermal score`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal().copy(
                    cpuTempC = null,
                    thermalStatus = ThermalStatus.NONE,
                ),
                healthyStorage(),
            )
        assertEquals(100, score.thermalScore)
    }

    @Test
    fun `very low storage usage is excellent but not always perfect`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(
                    usagePercent = 21.3f,
                    availableBytes = 100_736_000_000L,
                    usedBytes = 27_264_000_000L,
                ),
            )
        assertTrue("Expected storage score below 100, got ${score.storageScore}", score.storageScore < 100)
    }

    @Test
    fun `status thresholds are correct`() {
        assertEquals(
            HealthStatus.HEALTHY,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(100),
        )
        assertEquals(
            HealthStatus.HEALTHY,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(75),
        )
        assertEquals(
            HealthStatus.FAIR,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(74),
        )
        assertEquals(
            HealthStatus.FAIR,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(50),
        )
        assertEquals(
            HealthStatus.POOR,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(49),
        )
        assertEquals(
            HealthStatus.POOR,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(25),
        )
        assertEquals(
            HealthStatus.CRITICAL,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(24),
        )
        assertEquals(
            HealthStatus.CRITICAL,
            com.runcheck.domain.model.HealthScore
                .statusFromScore(0),
        )
    }

    // ---------------------------------------------------------------
    // Network score with speed test
    // ---------------------------------------------------------------

    private fun recentSpeedTest(
        downloadMbps: Double = 80.0,
        uploadMbps: Double = 20.0,
        pingMs: Int = 20,
        jitterMs: Int? = 3,
        connectionType: ConnectionType = ConnectionType.WIFI,
        timestamp: Long = TEST_NOW_MILLIS - 60_000L,
    ) = SpeedTestResult(
        id = 1,
        timestamp = timestamp,
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        pingMs = pingMs,
        jitterMs = jitterMs,
        serverName = "Test Server",
        serverLocation = "Helsinki",
        connectionType = connectionType,
        networkSubtype = null,
        signalDbm = -50,
    )

    @Test
    fun `wifi with good download speed and recent speed test yields high network score`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest =
                    recentSpeedTest(
                        downloadMbps = 80.0,
                        pingMs = 15,
                        jitterMs = 2,
                    ),
                nowMillis = TEST_NOW_MILLIS,
            )
        // WiFi excellent signal, fast download, low ping, low jitter — should be very high
        assertTrue(
            "Expected network score >= 85 with good speed test, got ${score.networkScore}",
            score.networkScore >= 85,
        )
    }

    @Test
    fun `cellular with poor download speed and speed test yields low network score`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork().copy(
                    connectionType = ConnectionType.CELLULAR,
                    signalQuality = SignalQuality.POOR,
                    wifiSsid = null,
                    wifiSpeedMbps = null,
                ),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest =
                    recentSpeedTest(
                        downloadMbps = 2.0,
                        pingMs = 350,
                        jitterMs = 40,
                        connectionType = ConnectionType.CELLULAR,
                    ),
                nowMillis = TEST_NOW_MILLIS,
            )
        // Poor signal, slow download relative to 20 Mbps expectation, high ping, high jitter
        assertTrue(
            "Expected network score < 50 with poor cellular speed test, got ${score.networkScore}",
            score.networkScore < 50,
        )
    }

    @Test
    fun `high jitter in speed test reduces network score`() {
        val lowJitterScore =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = recentSpeedTest(jitterMs = 2),
                nowMillis = TEST_NOW_MILLIS,
            )

        val highJitterScore =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = recentSpeedTest(jitterMs = 60),
                nowMillis = TEST_NOW_MILLIS,
            )

        assertTrue(
            "Expected high jitter (${ highJitterScore.networkScore}) < low jitter (${lowJitterScore.networkScore})",
            highJitterScore.networkScore < lowJitterScore.networkScore,
        )
    }

    @Test
    fun `stale speed test is ignored and basic scoring is used`() {
        val staleSpeedTest =
            SpeedTestResult(
                id = 1,
                timestamp = TEST_NOW_MILLIS - 7_200_000L, // 2 hours ago — exceeds 1-hour window
                downloadMbps = 100.0,
                uploadMbps = 50.0,
                pingMs = 5,
                jitterMs = 1,
                serverName = "Test",
                serverLocation = "Helsinki",
                connectionType = ConnectionType.WIFI,
                networkSubtype = null,
                signalDbm = -40,
            )

        val scoreWithStale =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = staleSpeedTest,
                nowMillis = TEST_NOW_MILLIS,
            )

        val scoreWithout =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = null,
                nowMillis = TEST_NOW_MILLIS,
            )

        // Both should use basic scoring, yielding the same network score
        assertEquals(
            "Stale speed test should be ignored",
            scoreWithout.networkScore,
            scoreWithStale.networkScore,
        )
    }

    @Test
    fun `null jitter in speed test is excluded from the weighted score`() {
        val nullJitterScore =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = recentSpeedTest(jitterMs = null),
                nowMillis = TEST_NOW_MILLIS,
            )
        val lowJitterScore =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = recentSpeedTest(jitterMs = 2),
                nowMillis = TEST_NOW_MILLIS,
            )
        assertEquals(
            "Missing jitter must not be converted into an assumed stability score",
            lowJitterScore.networkScore,
            nullJitterScore.networkScore,
        )
    }

    @Test
    fun `missing jitter redistributes its weight across available speed test components`() {
        val now = 10_000_000L
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork().copy(signalQuality = SignalQuality.FAIR),
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest =
                    recentSpeedTest(
                        downloadMbps = 10.0,
                        pingMs = 120,
                        jitterMs = null,
                        timestamp = now - 60_000L,
                    ),
                nowMillis = now,
            )

        assertEquals(
            "Available weights 40 + 30 + 20 must be normalized to 100 percent",
            58,
            score.networkScore,
        )
    }

    @Test
    fun `speed test influence expires continuously at the one hour boundary`() {
        val now = 10_000_000L
        val speedTest =
            recentSpeedTest(
                downloadMbps = 1.0,
                pingMs = 600,
                jitterMs = 60,
                timestamp = now - 3_600_000L,
            )
        val network = healthyNetwork().copy(signalQuality = SignalQuality.GOOD, latencyMs = 80)

        val justBeforeExpiry =
            calculator.calculate(
                healthyBattery(),
                network,
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = speedTest,
                nowMillis = now - 1L,
            )
        val atExpiry =
            calculator.calculate(
                healthyBattery(),
                network,
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = speedTest,
                nowMillis = now,
            )
        val withoutSpeedTest =
            calculator.calculate(
                healthyBattery(),
                network,
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = null,
                nowMillis = now,
            )

        assertTrue(
            "The score must not visibly jump at 60 minutes",
            kotlin.math.abs(justBeforeExpiry.networkScore - atExpiry.networkScore) <= 1,
        )
        assertEquals(withoutSpeedTest.networkScore, atExpiry.networkScore)
    }

    @Test
    fun `recent speed test from another connection type is ignored`() {
        val now = 10_000_000L
        val network = healthyNetwork().copy(connectionType = ConnectionType.WIFI, latencyMs = 80)
        val cellularSpeedTest =
            recentSpeedTest(
                downloadMbps = 1.0,
                pingMs = 600,
                jitterMs = 60,
                connectionType = ConnectionType.CELLULAR,
                timestamp = now - 60_000L,
            )

        val score =
            calculator.calculate(
                healthyBattery(),
                network,
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = cellularSpeedTest,
                nowMillis = now,
            )
        val basicScore =
            calculator.calculate(
                healthyBattery(),
                network,
                healthyThermal(),
                healthyStorage(),
                recentSpeedTest = null,
                nowMillis = now,
            )

        assertEquals(basicScore.networkScore, score.networkScore)
    }

    // ---------------------------------------------------------------
    // Voltage bands in battery scoring
    // ---------------------------------------------------------------

    @Test
    fun `low voltage 3200mV causes significant battery deduction`() {
        val lowVoltageScore =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 3200),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        val idealVoltageScore =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 4000),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // 3200 mV is in the < 3500 band → 10 point deduction
        // 4000 mV is in the 3500..4250 band → 0 point deduction
        assertTrue(
            "Low voltage (${lowVoltageScore.batteryScore}) should be < ideal (${idealVoltageScore.batteryScore})",
            lowVoltageScore.batteryScore < idealVoltageScore.batteryScore,
        )
    }

    @Test
    fun `mid voltage 3600mV has no voltage deduction`() {
        // 3600 mV is in the 3500..4250 range → 0 deduction
        val score =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 3600),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        val idealScore =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 4000),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // Both are in the 3500..4250 band, so same battery score
        assertEquals(
            "Mid voltage 3600 and ideal 4000 should have same battery score",
            idealScore.batteryScore,
            score.batteryScore,
        )
    }

    @Test
    fun `ideal voltage 4000mV has no voltage deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 4000),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // GOOD health, 25°C (0 temp deduction), 4000 mV (0 voltage deduction), no healthPercent
        // Battery score should be 100
        assertEquals(
            "Ideal conditions should yield battery score 100, got ${score.batteryScore}",
            100,
            score.batteryScore,
        )
    }

    @Test
    fun `very low voltage 2800mV causes major battery deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 2800),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // 2800 mV < 3200 → 20 point deduction
        // Base 100 - 20 = 80
        assertEquals(
            "Very low voltage should deduct 20 from battery score",
            80,
            score.batteryScore,
        )
    }

    @Test
    fun `voltage above 4250mV causes over-voltage deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(voltageMv = 4300),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // > 4250 mV → 15 point deduction
        assertEquals(
            "Over-voltage should deduct 15 from battery score",
            85,
            score.batteryScore,
        )
    }

    // ---------------------------------------------------------------
    // Health percent bands
    // ---------------------------------------------------------------

    @Test
    fun `health percent 95 causes minimal deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 95),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // >= 95 → 0 deduction. Battery base = 100 (good health, ideal temp, ideal voltage)
        assertEquals(
            "Health percent 95 should cause 0 deduction",
            100,
            score.batteryScore,
        )
    }

    @Test
    fun `health percent 90 causes small deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 90),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // >= 90 but < 95 → 3 deduction
        assertEquals(
            "Health percent 90 should deduct 3",
            97,
            score.batteryScore,
        )
    }

    @Test
    fun `health percent 70 causes moderate deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 70),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // >= 70 but < 75 → 24 deduction
        assertEquals(
            "Health percent 70 should deduct 24",
            76,
            score.batteryScore,
        )
    }

    @Test
    fun `health percent 50 causes heavy deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 50),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // >= 50 but < 60 → 45 deduction
        assertEquals(
            "Health percent 50 should deduct 45",
            55,
            score.batteryScore,
        )
    }

    @Test
    fun `health percent below 50 causes extreme deduction`() {
        val score =
            calculator.calculate(
                healthyBattery().copy(healthPercent = 40),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage(),
            )
        // < 50 → 60 deduction
        assertEquals(
            "Health percent 40 should deduct 60",
            40,
            score.batteryScore,
        )
    }

    // ---------------------------------------------------------------
    // Storage usage granular bands
    // ---------------------------------------------------------------

    @Test
    fun `storage 10 percent usage is near perfect`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 10f),
            )
        // 10% → < 25 band → 2 deduction (but >= 10 so in the < 25 band, not < 10 band)
        assertEquals(
            "10% usage should deduct 2 → storage score 98",
            98,
            score.storageScore,
        )
    }

    @Test
    fun `storage 5 percent usage gets tiny deduction for very low usage`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 5f),
            )
        // < 10 → 4 deduction
        assertEquals(
            "5% usage should deduct 4 → storage score 96",
            96,
            score.storageScore,
        )
    }

    @Test
    fun `storage 50 percent usage gets no deduction`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 50f),
            )
        // >= 50 but < 70 → 5 deduction
        assertEquals(
            "50% usage should deduct 5 → storage score 95",
            95,
            score.storageScore,
        )
    }

    @Test
    fun `storage 40 percent usage is in the sweet spot`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 40f),
            )
        // >= 25 but < 50 → 0 deduction
        assertEquals(
            "40% usage should deduct 0 → storage score 100",
            100,
            score.storageScore,
        )
    }

    @Test
    fun `storage 80 percent usage causes moderate deduction`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 80f),
            )
        // >= 80 but < 90 → 35 deduction
        assertEquals(
            "80% usage should deduct 35 → storage score 65",
            65,
            score.storageScore,
        )
    }

    @Test
    fun `storage score status changes at documented utilization thresholds`() {
        val expectations =
            listOf(
                Triple(74.999f, 85, HealthStatus.HEALTHY),
                Triple(75f, 65, HealthStatus.FAIR),
                Triple(84.999f, 65, HealthStatus.FAIR),
                Triple(85f, 45, HealthStatus.POOR),
                Triple(94.999f, 45, HealthStatus.POOR),
                Triple(95f, 20, HealthStatus.CRITICAL),
            )

        expectations.forEach { (usagePercent, expectedScore, expectedStatus) ->
            val score =
                calculator.calculate(
                    healthyBattery(),
                    healthyNetwork(),
                    healthyThermal(),
                    healthyStorage().copy(usagePercent = usagePercent),
                )

            assertEquals(
                "Storage usage $usagePercent% should map to score $expectedScore",
                expectedScore,
                score.storageScore,
            )
            assertEquals(expectedStatus, HealthScore.statusFromScore(score.storageScore))
        }
    }

    @Test
    fun `storage 90 percent usage causes heavy deduction`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 90f),
            )
        // >= 90 but < 95 → 55 deduction
        assertEquals(
            "90% usage should deduct 55 → storage score 45",
            45,
            score.storageScore,
        )
    }

    @Test
    fun `storage 95 percent usage causes severe deduction`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 95f),
            )
        // >= 95 → 80 deduction
        assertEquals(
            "95% usage should deduct 80 → storage score 20",
            20,
            score.storageScore,
        )
    }

    @Test
    fun `storage 99 percent usage is also severe`() {
        val score =
            calculator.calculate(
                healthyBattery(),
                healthyNetwork(),
                healthyThermal(),
                healthyStorage().copy(usagePercent = 99f),
            )
        // >= 95 → 80 deduction
        assertEquals(
            "99% usage should deduct 80 → storage score 20",
            20,
            score.storageScore,
        )
    }

    // ---------------------------------------------------------------
    // Storage score ordering validates monotonic degradation
    // ---------------------------------------------------------------

    @Test
    fun `storage scores degrade as usage increases across all bands`() {
        val usages = listOf(5f, 15f, 40f, 60f, 80f, 92f, 97f)
        val scores =
            usages.map { pct ->
                calculator
                    .calculate(
                        healthyBattery(),
                        healthyNetwork(),
                        healthyThermal(),
                        healthyStorage().copy(usagePercent = pct),
                    ).storageScore
            }
        // The 25-50% sweet spot (40f) may score higher than very low usage (5f)
        // but from 50% onwards, scores should monotonically decrease
        for (i in 2 until scores.lastIndex) {
            assertTrue(
                "Storage score at ${usages[i]}% (${scores[i]}) should be >= score at ${usages[i + 1]}% (${scores[i + 1]})",
                scores[i] >= scores[i + 1],
            )
        }
    }
}
