package com.runcheck.domain.usecase

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
import com.runcheck.domain.scoring.HealthScoreCalculator
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalculateHealthScoreUseCaseTest {

    private lateinit var useCase: CalculateHealthScoreUseCase
    private lateinit var getBatteryState: GetBatteryStateUseCase
    private lateinit var getNetworkState: GetNetworkStateUseCase
    private lateinit var getThermalState: GetThermalStateUseCase
    private lateinit var getStorageState: GetStorageStateUseCase

    private val battery = BatteryState(
        level = 80,
        voltageMv = 4000,
        temperatureC = 28f,
        currentMa = MeasuredValue(400, Confidence.HIGH),
        chargingStatus = ChargingStatus.DISCHARGING,
        plugType = PlugType.NONE,
        health = BatteryHealth.GOOD,
        technology = "Li-ion"
    )

    private val network = NetworkState(
        connectionType = ConnectionType.WIFI,
        signalDbm = -50,
        signalQuality = SignalQuality.EXCELLENT,
        latencyMs = 20
    )

    private val thermal = ThermalState(
        batteryTempC = 30f,
        cpuTempC = 42f,
        thermalStatus = ThermalStatus.NONE,
        isThrottling = false
    )

    private val storage = StorageState(
        totalBytes = 128_000_000_000L,
        availableBytes = 64_000_000_000L,
        usedBytes = 64_000_000_000L,
        usagePercent = 50f,
        appsBytes = 20_000_000_000L,
        mediaBytes = 30_000_000_000L
    )

    @Before
    fun setup() {
        getBatteryState = mockk()
        getNetworkState = mockk()
        getThermalState = mockk()
        getStorageState = mockk()

        every { getBatteryState() } returns flowOf(battery)
        every { getNetworkState() } returns flowOf(network)
        every { getThermalState() } returns flowOf(thermal)
        every { getStorageState() } returns flowOf(storage)

        useCase = CalculateHealthScoreUseCase(
            getBatteryState = getBatteryState,
            getNetworkState = getNetworkState,
            getThermalState = getThermalState,
            getStorageState = getStorageState,
            calculator = HealthScoreCalculator()
        )
    }

    @Test
    fun `emits health score from combined state flows`() = runTest {
        val score = useCase().first()

        assertTrue(score.overallScore in 0..100)
        assertTrue(score.batteryScore in 0..100)
        assertTrue(score.networkScore in 0..100)
        assertTrue(score.thermalScore in 0..100)
        assertTrue(score.storageScore in 0..100)
    }

    @Test
    fun `healthy inputs produce healthy status`() = runTest {
        val score = useCase().first()

        assertEquals(HealthStatus.HEALTHY, score.status)
        assertTrue(score.overallScore >= 75)
    }

    @Test
    fun `degraded inputs lower overall score`() = runTest {
        every { getNetworkState() } returns flowOf(
            network.copy(
                connectionType = ConnectionType.NONE,
                signalDbm = -999,
                signalQuality = SignalQuality.NO_SIGNAL
            )
        )
        every { getThermalState() } returns flowOf(
            thermal.copy(
                batteryTempC = 46f,
                thermalStatus = ThermalStatus.SEVERE,
                isThrottling = true
            )
        )

        useCase = CalculateHealthScoreUseCase(
            getBatteryState = getBatteryState,
            getNetworkState = getNetworkState,
            getThermalState = getThermalState,
            getStorageState = getStorageState,
            calculator = HealthScoreCalculator()
        )

        val score = useCase().first()

        assertTrue("Expected degraded score <75, got ${score.overallScore}", score.overallScore < 75)
        assertEquals(0, score.networkScore)
    }
}
