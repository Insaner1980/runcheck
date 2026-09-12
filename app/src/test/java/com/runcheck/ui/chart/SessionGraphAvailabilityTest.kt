package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionGraphAvailabilityTest {
    @Test
    fun `availability matches both graph metrics for missing and present currents`() {
        val currents = listOf(null, 0, -100, 1_200)
        for (first in currents) {
            for (second in currents) {
                for (third in currents) {
                    val readings = listOf(first, second, third).mapIndexed { index, current -> reading(index, current) }
                    for (size in 0..readings.size) {
                        val summary = summary(readings.take(size))
                        val expected =
                            SessionGraphMetric.entries.any { metric ->
                                summary.readings.graphPointsFor(metric, SessionGraphWindow.ALL).size >= 2
                            }
                        assertEquals(expected, summary.hasGraphData())
                    }
                }
            }
        }
    }

    @Test
    fun `availability stops reading once two valid points are found`() {
        var reads = 0
        val readings =
            object : AbstractList<BatteryReading>() {
                override val size = 2_880

                override fun get(index: Int): BatteryReading {
                    reads++
                    return reading(index, if (index == 0) null else 1_200)
                }
            }

        assertTrue(summary(readings).hasGraphData())
        assertEquals(3, reads)
    }

    private fun summary(readings: List<BatteryReading>) =
        ChargingSessionSummary(
            startLevel = 50,
            gainPercent = 0,
            durationMs = 0,
            peakTemperatureC = 30f,
            averageCurrentMa = null,
            deliveredMah = null,
            averagePowerW = null,
            averageSpeedPctPerHour = null,
            recentSpeedPctPerHour = null,
            remainingTo80Ms = null,
            remainingTo100Ms = null,
            readings = readings,
        )

    private fun reading(
        index: Int,
        current: Int?,
    ) = BatteryReading(
        timestamp = index * 60_000L,
        level = 50,
        voltageMv = 4_000,
        temperatureC = 30f,
        currentMa = current,
        currentConfidence = "ACCURATE",
        status = "CHARGING",
        plugType = "USB",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )
}
