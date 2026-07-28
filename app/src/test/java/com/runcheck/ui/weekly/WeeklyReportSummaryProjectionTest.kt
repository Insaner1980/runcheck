package com.runcheck.ui.weekly

import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyReportCoverage
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklySpeedSummary
import com.runcheck.domain.model.WeeklyStorageSummary
import com.runcheck.domain.model.WeeklyThermalSummary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WeeklyReportSummaryProjectionTest {
    @Test
    fun `summary projection exposes only existing weekly report values`() {
        val report =
            WeeklyReport(
                period =
                    WeeklyReportPeriod(
                        startInclusive = Instant.parse("2026-07-13T00:00:00Z"),
                        endExclusive = Instant.parse("2026-07-20T00:00:00Z"),
                        zoneId = ZoneId.of("UTC"),
                    ),
                coverage =
                    WeeklyReportCoverage(
                        monitoredDays = 6,
                        sampleCount = 84,
                        availability = WeeklyReportAvailability.ESTIMATED,
                    ),
                battery =
                    WeeklyBatterySummary(
                        averageDischargePercentPerHour = null,
                        dischargePercentChange = 0.0,
                        chargePercentChange = 0.0,
                        healthPercentChange = null,
                        availability = WeeklyReportAvailability.UNAVAILABLE,
                        validSegmentCount = 0,
                    ),
                storage =
                    WeeklyStorageSummary(
                        availableBytesChange = null,
                        availability = WeeklyReportAvailability.UNAVAILABLE,
                    ),
                thermal =
                    WeeklyThermalSummary(
                        throttlingEventCount = 0,
                        highestThermalStatus = null,
                        availability = WeeklyReportAvailability.UNAVAILABLE,
                    ),
                speed =
                    WeeklySpeedSummary(
                        testCount = 3,
                        medianDownloadMbps = 100.0,
                        medianUploadMbps = 20.0,
                        medianLatencyMs = 15.0,
                        availability = WeeklyReportAvailability.AVAILABLE,
                    ),
                topApps = emptyList(),
                availability = WeeklyReportAvailability.ESTIMATED,
            )

        assertEquals(
            WeeklyReportSummaryProjection(
                monitoredDays = 6,
                sampleCount = 84,
                speedTestCount = 3,
                availability = WeeklyReportAvailability.ESTIMATED,
            ),
            weeklyReportSummaryProjection(report),
        )
    }
}
