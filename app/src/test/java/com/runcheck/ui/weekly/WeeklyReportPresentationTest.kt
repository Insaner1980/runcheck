package com.runcheck.ui.weekly

import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyThermalSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class WeeklyReportPresentationTest {
    @Test
    fun `unavailable battery suppresses zero-valued level changes`() {
        assertEquals(
            WeeklyReportMetricProjection.Unavailable,
            batteryProjection(
                WeeklyBatterySummary(
                    averageDischargePercentPerHour = null,
                    dischargePercentChange = 0.0,
                    chargePercentChange = 0.0,
                    healthPercentChange = null,
                    availability = WeeklyReportAvailability.UNAVAILABLE,
                    validSegmentCount = 0,
                ),
            ),
        )
    }

    @Test
    fun `unavailable thermal data suppresses a zero throttling event claim`() {
        assertEquals(
            WeeklyReportMetricProjection.Unavailable,
            thermalProjection(
                WeeklyThermalSummary(
                    throttlingEventCount = 0,
                    highestThermalStatus = null,
                    availability = WeeklyReportAvailability.UNAVAILABLE,
                ),
            ),
        )
    }
}
