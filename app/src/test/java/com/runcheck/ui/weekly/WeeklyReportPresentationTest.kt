package com.runcheck.ui.weekly

import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyThermalSummary
import com.runcheck.util.readContractText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

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

    @Test
    fun `estimated app usage explains both endpoint attribution limits`() {
        val content =
            File("src/main/java/com/runcheck/ui/weekly/WeeklyReportContent.kt").readContractText()
        val strings = File("src/main/res/values/strings.xml").readContractText()

        assertTrue(content.contains("weekly_report_app_usage_endpoint_attributed"))
        assertTrue(strings.contains("include activity from outside this week"))
        assertTrue(strings.contains("omit in-week activity"))
    }
}
