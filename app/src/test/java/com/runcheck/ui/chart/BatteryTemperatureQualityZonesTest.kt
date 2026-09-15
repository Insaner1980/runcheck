package com.runcheck.ui.chart

import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.ui.theme.RuncheckStatusColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryTemperatureQualityZonesTest {
    private val colors = RuncheckStatusColors

    @Test
    fun `battery and thermal battery history share four severity zones`() {
        TemperatureUnit.entries.forEach { unit ->
            val zones = batteryTemperatureQualityZones(unit, colors)
            assertEquals(zones, thermalQualityZones(ThermalHistoryMetric.BATTERY_TEMP, unit, colors))
            val boundaries =
                if (unit == TemperatureUnit.CELSIUS) {
                    listOf(0f, 35f, 40f, 45f, 60f)
                } else {
                    listOf(32f, 95f, 104f, 113f, 140f)
                }
            assertEquals(boundaries.dropLast(1), zones.map { it.minValue })
            assertEquals(boundaries.drop(1), zones.map { it.maxValue })
            assertEquals(
                listOf(colors.healthy, colors.fair, colors.poor, colors.critical),
                zones.map { it.color.copy(alpha = 1f) },
            )
            zones.forEach { assertEquals(0.06f, it.color.alpha, 0.005f) }
        }
        val zones = batteryTemperatureQualityZones(TemperatureUnit.CELSIUS, colors)
        listOf(40f, 42f, 43f, 44.9f).forEach {
            assertEquals(colors.poor, qualityZoneColorForValue(it, zones, colors.neutral))
        }
        assertEquals(colors.healthy, qualityZoneColorForValue(34.9f, zones, colors.neutral))
        assertEquals(colors.critical, qualityZoneColorForValue(45f, zones, colors.neutral))
    }

    @Test
    fun `CPU history never inherits battery quality zones`() {
        TemperatureUnit.entries.forEach {
            assertNull(thermalQualityZones(ThermalHistoryMetric.CPU_TEMP, it, colors))
        }
    }
}
