package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
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

    private fun batteryReading(
        timestamp: Long,
        level: Int,
        temperatureC: Float,
    ) = BatteryReading(
        id = timestamp,
        timestamp = timestamp,
        level = level,
        voltageMv = 4_000,
        temperatureC = temperatureC,
        currentMa = 1_200,
        currentConfidence = "HIGH",
        status = "CHARGING",
        plugType = "USB",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )
}
