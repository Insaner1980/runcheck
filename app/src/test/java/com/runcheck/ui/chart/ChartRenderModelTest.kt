package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartRenderModelTest {
    @Test
    fun `battery temperature chart honors fahrenheit preference`() {
        val history =
            listOf(
                batteryReading(timestamp = 1_000L, level = 80, temperatureC = 30f),
                batteryReading(timestamp = 2_000L, level = 79, temperatureC = 31.5f),
                batteryReading(timestamp = 3_000L, level = 78, temperatureC = 33f),
            )

        val points =
            history.chartPointsFor(
                metric = BatteryHistoryMetric.TEMPERATURE,
                temperatureUnit = TemperatureUnit.FAHRENHEIT,
            )

        assertEquals("°F", batteryMetricUnit(BatteryHistoryMetric.TEMPERATURE, TemperatureUnit.FAHRENHEIT))
        assertEquals(86.0f, points.first().second, 0.01f)
        assertEquals(91.4f, points.last().second, 0.01f)
    }

    @Test
    fun `network history chart downsampling keeps bounded point count`() {
        val points =
            (0 until 1_000)
                .map { index ->
                    index * 60_000L to (-40 - (index % 30)).toFloat()
                }.downsamplePairs(300)

        assertEquals(300, points.size)
        assertEquals(0L, points.first().first)
        assertEquals(999 * 60_000L, points.last().first)
        assertTrue(points.zipWithNext().all { (first, second) -> first.first <= second.first })
    }

    @Test
    fun `charging session summary uses latest contiguous charging session`() {
        val history =
            listOf(
                batteryReading(timestamp = 0L, level = 85, status = "DISCHARGING"),
                batteryReading(timestamp = 10_000L, level = 60),
                batteryReading(timestamp = 15 * 60_000L + 10_000L, level = 65, temperatureC = 34f),
                batteryReading(timestamp = 30 * 60_000L + 10_000L, level = 70, temperatureC = 36f),
            )

        val summary =
            calculateChargingSessionSummary(
                history = history,
                currentLevel = 70,
                chargingStatus = ChargingStatus.CHARGING,
            )

        assertNotNull(summary)
        assertEquals(60, summary?.startLevel)
        assertEquals(10, summary?.gainPercent)
        assertEquals(30 * 60_000L, summary?.durationMs)
        assertEquals(36f, summary?.peakTemperatureC ?: 0f, 0.01f)
        assertEquals(600, summary?.deliveredMah)
        assertEquals(1_200, summary?.averageCurrentMa)
        assertEquals(20f, summary?.averageSpeedPctPerHour ?: 0f, 0.01f)
        assertEquals(20f, summary?.recentSpeedPctPerHour ?: 0f, 0.01f)
        assertEquals(30 * 60_000L, summary?.remainingTo80Ms)
        assertEquals(90 * 60_000L, summary?.remainingTo100Ms)
    }

    @Test
    fun `session graph points apply selected time window and power conversion`() {
        val history =
            listOf(
                batteryReading(timestamp = 0L, level = 50),
                batteryReading(timestamp = 10 * 60_000L, level = 52),
                batteryReading(timestamp = 20 * 60_000L, level = 54),
                batteryReading(timestamp = 40 * 60_000L, level = 58),
            )

        val points =
            history.graphPointsFor(
                metric = SessionGraphMetric.POWER,
                window = SessionGraphWindow.THIRTY_MINUTES,
            )

        assertEquals(3, points.size)
        assertEquals(10 * 60_000L, points.first().first)
        assertEquals(4.8f, points.first().second, 0.01f)
        assertEquals(40 * 60_000L, points.last().first)
    }

    private fun batteryReading(
        timestamp: Long,
        level: Int,
        temperatureC: Float = 30f,
        status: String = "CHARGING",
    ) = BatteryReading(
        id = timestamp,
        timestamp = timestamp,
        level = level,
        voltageMv = 4_000,
        temperatureC = temperatureC,
        currentMa = 1_200,
        currentConfidence = "HIGH",
        status = status,
        plugType = "USB",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )
}
