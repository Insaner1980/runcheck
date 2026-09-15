package com.runcheck.ui.common

import com.runcheck.domain.model.HealthStatus
import com.runcheck.ui.common.BatteryTemperaturePresentation.Band
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryTemperaturePresentationTest {
    @Test
    fun `raw Celsius bands and severity follow approved boundaries`() {
        val cases =
            listOf(
                Triple(24f, Band.COOL, HealthStatus.HEALTHY),
                Triple(25f, Band.NORMAL, HealthStatus.HEALTHY),
                Triple(34.9f, Band.NORMAL, HealthStatus.HEALTHY),
                Triple(35f, Band.WARM, HealthStatus.FAIR),
                Triple(37f, Band.WARM, HealthStatus.FAIR),
                Triple(39.9f, Band.WARM, HealthStatus.FAIR),
                Triple(40f, Band.HOT, HealthStatus.POOR),
                Triple(42f, Band.HOT, HealthStatus.POOR),
                Triple(43f, Band.HOT, HealthStatus.POOR),
                Triple(44.9f, Band.HOT, HealthStatus.POOR),
                Triple(45f, Band.CRITICAL, HealthStatus.CRITICAL),
                Triple(46f, Band.CRITICAL, HealthStatus.CRITICAL),
            )
        cases.forEach { (celsius, band, severity) ->
            val actual = BatteryTemperaturePresentation.classify(celsius)
            assertEquals("band at $celsius", band, actual)
            assertEquals("severity at $celsius", severity, actual.severity)
        }
    }

    @Test
    fun `adjacent floats are classified before display rounding`() {
        listOf(
            Triple(25f, Band.COOL, Band.NORMAL),
            Triple(35f, Band.NORMAL, Band.WARM),
            Triple(40f, Band.WARM, Band.HOT),
            Triple(45f, Band.HOT, Band.CRITICAL),
        ).forEach { (boundary, below, atOrAbove) ->
            assertEquals(below, BatteryTemperaturePresentation.classify(Math.nextDown(boundary)))
            assertEquals(atOrAbove, BatteryTemperaturePresentation.classify(boundary))
            assertEquals(atOrAbove, BatteryTemperaturePresentation.classify(Math.nextUp(boundary)))
        }
    }
}
