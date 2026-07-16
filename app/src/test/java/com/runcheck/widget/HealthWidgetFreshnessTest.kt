package com.runcheck.widget

import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.scoring.HealthScoreCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthWidgetFreshnessTest {
    private val calculator = HealthScoreCalculator()

    @Test
    fun `coherent fresh readings produce content independent of timestamp assignment`() {
        val first = renderState(0L, -1_000L, -2_000L, -3_000L)
        val reordered = renderState(-3_000L, -2_000L, -1_000L, 0L)

        assertTrue(first is WidgetRenderState.Content)
        assertTrue(reordered is WidgetRenderState.Content)
        assertEquals(
            (first as WidgetRenderState.Content).snapshot,
            (reordered as WidgetRenderState.Content).snapshot,
        )
    }

    @Test
    fun `readings outside the coherent input window are disclosed as stale`() {
        val state = renderState(0L, -1_000L, -2_000L, -120_001L)

        assertEquals(WidgetRenderState.Stale, state)
    }

    @Test
    fun `readings older than configured monitoring freshness are disclosed as stale`() {
        val tooOld = -(MonitoringInterval.FIFTEEN.minutes * 60_000L * 3L + 1L)
        val state = renderState(tooOld, tooOld, tooOld, tooOld)

        assertEquals(WidgetRenderState.Stale, state)
    }

    @Test
    fun `future timestamps are disclosed as stale`() {
        val state = renderState(1L, 0L, 0L, 0L)

        assertEquals(WidgetRenderState.Stale, state)
    }

    private fun renderState(
        batteryOffset: Long,
        networkOffset: Long,
        thermalOffset: Long,
        storageOffset: Long,
    ): WidgetRenderState<HealthWidgetSnapshot> =
        healthWidgetRenderState(
            isPro = true,
            monitoringInterval = MonitoringInterval.FIFTEEN,
            readings =
                HealthWidgetReadings(
                    battery = batteryReading(NOW_MILLIS + batteryOffset),
                    network = networkReading(NOW_MILLIS + networkOffset),
                    thermal = thermalReading(NOW_MILLIS + thermalOffset),
                    storage = storageReading(NOW_MILLIS + storageOffset),
                ),
            nowMillis = NOW_MILLIS,
            calculator = calculator,
        )

    private fun batteryReading(timestamp: Long) =
        BatteryReadingEntity(
            timestamp = timestamp,
            level = 80,
            voltageMv = 4_000,
            temperatureC = 25f,
            currentMa = 100,
            currentConfidence = Confidence.HIGH.name,
            status = ChargingStatus.DISCHARGING.name,
            plugType = PlugType.NONE.name,
            health = BatteryHealth.GOOD.name,
            cycleCount = 100,
            healthPct = 95,
        )

    private fun networkReading(timestamp: Long) =
        NetworkReadingEntity(
            timestamp = timestamp,
            type = ConnectionType.WIFI.name,
            signalDbm = -50,
            wifiSpeedMbps = 300,
            wifiFrequency = 5_000,
            carrier = null,
            networkSubtype = null,
            latencyMs = 20,
        )

    private fun thermalReading(timestamp: Long) =
        ThermalReadingEntity(
            timestamp = timestamp,
            batteryTempC = 25f,
            cpuTempC = 40f,
            thermalStatus = 0,
            throttling = false,
        )

    private fun storageReading(timestamp: Long) =
        StorageReadingEntity(
            timestamp = timestamp,
            totalBytes = 100L,
            availableBytes = 60L,
            appsBytes = 20L,
            mediaBytes = 10L,
        )

    private companion object {
        const val NOW_MILLIS = 100_000_000L
    }
}
