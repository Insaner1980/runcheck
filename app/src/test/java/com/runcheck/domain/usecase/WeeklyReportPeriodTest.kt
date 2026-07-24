package com.runcheck.domain.usecase

import com.runcheck.domain.model.WeeklyReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class WeeklyReportPeriodTest {
    @Test
    fun `previous completed week uses local Monday boundaries`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = ZonedDateTime.of(2026, 2, 18, 12, 0, 0, 0, zone).toInstant()

        val period = WeeklyReportPeriod.previousCompleted(now, zone)

        assertEquals(
            ZonedDateTime.of(2026, 2, 9, 0, 0, 0, 0, zone).toInstant(),
            period.startInclusive,
        )
        assertEquals(
            ZonedDateTime.of(2026, 2, 16, 0, 0, 0, 0, zone).toInstant(),
            period.endExclusive,
        )
        assertEquals(zone, period.zoneId)
    }

    @Test
    fun `spring DST week remains local Monday to Monday`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = Instant.parse("2026-03-30T08:00:00Z")

        val period = WeeklyReportPeriod.previousCompleted(now, zone)

        assertEquals(167, Duration.between(period.startInclusive, period.endExclusive).toHours())
        assertEquals(23, period.startInclusive.atZone(zone).dayOfMonth)
        assertEquals(30, period.endExclusive.atZone(zone).dayOfMonth)
    }

    @Test
    fun `autumn DST week remains local Monday to Monday`() {
        val zone = ZoneId.of("Europe/Helsinki")
        val now = Instant.parse("2026-10-26T08:00:00Z")

        val period = WeeklyReportPeriod.previousCompleted(now, zone)

        assertEquals(169, Duration.between(period.startInclusive, period.endExclusive).toHours())
        assertEquals(19, period.startInclusive.atZone(zone).dayOfMonth)
        assertEquals(26, period.endExclusive.atZone(zone).dayOfMonth)
    }

    @Test
    fun `same instant follows the requested current zone`() {
        val instant = Instant.parse("2026-01-05T02:00:00Z")

        val helsinki = WeeklyReportPeriod.previousCompleted(instant, ZoneId.of("Europe/Helsinki"))
        val losAngeles = WeeklyReportPeriod.previousCompleted(instant, ZoneId.of("America/Los_Angeles"))

        assertEquals(Instant.parse("2025-12-28T22:00:00Z"), helsinki.startInclusive)
        assertEquals(Instant.parse("2025-12-22T08:00:00Z"), losAngeles.startInclusive)
    }
}
