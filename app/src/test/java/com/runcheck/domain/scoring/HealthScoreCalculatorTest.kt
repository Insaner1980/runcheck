package com.runcheck.domain.scoring

import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.HealthStatus
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HealthScoreCalculatorTest {

    private lateinit var calculator: HealthScoreCalculator

    @Before
    fun setup() {
        calculator = HealthScoreCalculator()
    }

    private fun healthyBattery() = BatteryState(
        level = 85,
        voltageMv = 4100,
        temperatureC = 25f,
        currentMa = MeasuredValue(500, Confidence.HIGH),
        chargingStatus = ChargingStatus.DISCHARGING,
        plugType = PlugType.NONE,
        health = BatteryHealth.GOOD,
        technology = "Li-ion"
    )

    private fun healthyNetwork() = NetworkState(
        connectionType = ConnectionType.WIFI,
        signalDbm = -45,
        signalQuality = SignalQuality.EXCELLENT,
        wifiSsid = "Home",
        wifiSpeedMbps = 300,
        latencyMs = 15
    )

    private fun healthyThermal() = ThermalState(
        batteryTempC = 28f,
        cpuTempC = 40f,
        thermalStatus = ThermalStatus.NONE,
        isThrottling = false
    )

    private fun healthyStorage() = StorageState(
        totalBytes = 128_000_000_000L,
        availableBytes = 80_000_000_000L,
        usedBytes = 48_000_000_000L,
        usagePercent = 37.5f,
        appsBytes = 16_000_000_000L,
        mediaBytes = 16_000_000_000L
    )

    @Test
    fun `healthy device scores 75+`() {
        val score = calculator.calculate(
            healthyBattery(), healthyNetwork(), healthyThermal(), healthyStorage()
        )
        assertTrue("Expected healthy score >=75, got ${score.overallScore}", score.overallScore >= 75)
        assertEquals(HealthStatus.HEALTHY, score.status)
    }

    @Test
    fun `no network connection drops network score to 0`() {
        val score = calculator.calculate(
            healthyBattery(),
            healthyNetwork().copy(
                connectionType = ConnectionType.NONE,
                signalDbm = -999,
                signalQuality = SignalQuality.NO_SIGNAL
            ),
            healthyThermal(),
            healthyStorage()
        )
        assertEquals(0, score.networkScore)
    }

    @Test
    fun `overheating battery reduces battery score`() {
        val score = calculator.calculate(
            healthyBattery().copy(
                temperatureC = 48f,
                health = BatteryHealth.OVERHEAT
            ),
            healthyNetwork(),
            healthyThermal(),
            healthyStorage()
        )
        assertTrue("Expected degraded battery score, got ${score.batteryScore}", score.batteryScore < 50)
    }

    @Test
    fun `critical thermal status reduces thermal score`() {
        val score = calculator.calculate(
            healthyBattery(),
            healthyNetwork(),
            healthyThermal().copy(
                batteryTempC = 46f,
                thermalStatus = ThermalStatus.CRITICAL,
                isThrottling = true
            ),
            healthyStorage()
        )
        assertTrue("Expected poor thermal score, got ${score.thermalScore}", score.thermalScore < 25)
    }

    @Test
    fun `nearly full storage reduces storage score`() {
        val score = calculator.calculate(
            healthyBattery(),
            healthyNetwork(),
            healthyThermal(),
            healthyStorage().copy(
                usagePercent = 96f,
                availableBytes = 5_000_000_000L,
                usedBytes = 123_000_000_000L
            )
        )
        assertTrue("Expected poor storage score, got ${score.storageScore}", score.storageScore < 30)
    }

    @Test
    fun `overall score is weighted average`() {
        val score = calculator.calculate(
            healthyBattery(), healthyNetwork(), healthyThermal(), healthyStorage()
        )
        // Overall should be roughly the weighted average of sub-scores
        val expectedApprox = (
            score.batteryScore * 0.35 +
            score.networkScore * 0.20 +
            score.thermalScore * 0.25 +
            score.storageScore * 0.20
        ).toInt()
        assertEquals(expectedApprox, score.overallScore)
    }

    @Test
    fun `scores are clamped to 0-100`() {
        val score = calculator.calculate(
            healthyBattery().copy(
                health = BatteryHealth.DEAD,
                temperatureC = 55f,
                voltageMv = 2800
            ),
            healthyNetwork().copy(
                connectionType = ConnectionType.NONE,
                signalDbm = -999,
                signalQuality = SignalQuality.NO_SIGNAL
            ),
            healthyThermal().copy(
                batteryTempC = 55f,
                thermalStatus = ThermalStatus.SHUTDOWN,
                isThrottling = true
            ),
            healthyStorage().copy(usagePercent = 99f)
        )
        assertTrue(score.overallScore in 0..100)
        assertTrue(score.batteryScore in 0..100)
        assertTrue(score.networkScore in 0..100)
        assertTrue(score.thermalScore in 0..100)
        assertTrue(score.storageScore in 0..100)
    }

    @Test
    fun `battery health percent of 80 is not treated as perfect`() {
        val score = calculator.calculate(
            healthyBattery().copy(
                level = 80,
                temperatureC = 33.6f,
                healthPercent = 80
            ),
            healthyNetwork(),
            healthyThermal(),
            healthyStorage()
        )
        assertTrue("Expected battery score below 100, got ${score.batteryScore}", score.batteryScore < 100)
    }

    @Test
    fun `good thermal reading is not automatically perfect`() {
        val score = calculator.calculate(
            healthyBattery(),
            healthyNetwork(),
            healthyThermal().copy(
                batteryTempC = 33.6f,
                cpuTempC = null,
                thermalStatus = ThermalStatus.NONE
            ),
            healthyStorage()
        )
        assertTrue("Expected thermal score below 100, got ${score.thermalScore}", score.thermalScore < 100)
    }

    @Test
    fun `very low storage usage is excellent but not always perfect`() {
        val score = calculator.calculate(
            healthyBattery(),
            healthyNetwork(),
            healthyThermal(),
            healthyStorage().copy(
                usagePercent = 21.3f,
                availableBytes = 100_736_000_000L,
                usedBytes = 27_264_000_000L
            )
        )
        assertTrue("Expected storage score below 100, got ${score.storageScore}", score.storageScore < 100)
    }

    @Test
    fun `status thresholds are correct`() {
        assertEquals(HealthStatus.HEALTHY, com.runcheck.domain.model.HealthScore.statusFromScore(100))
        assertEquals(HealthStatus.HEALTHY, com.runcheck.domain.model.HealthScore.statusFromScore(75))
        assertEquals(HealthStatus.FAIR, com.runcheck.domain.model.HealthScore.statusFromScore(74))
        assertEquals(HealthStatus.FAIR, com.runcheck.domain.model.HealthScore.statusFromScore(50))
        assertEquals(HealthStatus.POOR, com.runcheck.domain.model.HealthScore.statusFromScore(49))
        assertEquals(HealthStatus.POOR, com.runcheck.domain.model.HealthScore.statusFromScore(25))
        assertEquals(HealthStatus.CRITICAL, com.runcheck.domain.model.HealthScore.statusFromScore(24))
        assertEquals(HealthStatus.CRITICAL, com.runcheck.domain.model.HealthScore.statusFromScore(0))
    }
}
