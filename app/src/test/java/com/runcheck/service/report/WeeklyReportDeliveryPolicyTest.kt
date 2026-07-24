package com.runcheck.service.report

import com.runcheck.domain.model.WeeklyReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class WeeklyReportDeliveryPolicyTest {
    private val period =
        WeeklyReportPeriod(
            Instant.ofEpochMilli(100),
            Instant.ofEpochMilli(200),
            ZoneId.of("UTC"),
        )

    @Test
    fun `already processed period is skipped without duplicate notification`() {
        assertEquals(
            WeeklyReportDeliveryDecision.ALREADY_PROCESSED,
            decideWeeklyReportDelivery(
                period = period,
                lastProcessedStart = 100,
                lastProcessedEnd = 200,
                weeklyEnabled = true,
                hasPro = true,
                notificationsEnabled = true,
                canPostNotifications = true,
                reportsChannelEnabled = true,
            ),
        )
    }

    @Test
    fun `notification denial is a terminal handled decision`() {
        assertEquals(
            WeeklyReportDeliveryDecision.HANDLE_WITHOUT_NOTIFICATION,
            decideWeeklyReportDelivery(
                period = period,
                lastProcessedStart = null,
                lastProcessedEnd = null,
                weeklyEnabled = true,
                hasPro = true,
                notificationsEnabled = true,
                canPostNotifications = false,
                reportsChannelEnabled = true,
            ),
        )
    }

    @Test
    fun `master notification toggle marks the period handled`() {
        assertEquals(
            WeeklyReportDeliveryDecision.HANDLE_WITHOUT_NOTIFICATION,
            decideWeeklyReportDelivery(
                period = period,
                lastProcessedStart = null,
                lastProcessedEnd = null,
                weeklyEnabled = true,
                hasPro = true,
                notificationsEnabled = false,
                canPostNotifications = true,
                reportsChannelEnabled = true,
            ),
        )
    }

    @Test
    fun `disabled reports channel marks the period handled`() {
        assertEquals(
            WeeklyReportDeliveryDecision.HANDLE_WITHOUT_NOTIFICATION,
            decideWeeklyReportDelivery(
                period = period,
                lastProcessedStart = null,
                lastProcessedEnd = null,
                weeklyEnabled = true,
                hasPro = true,
                notificationsEnabled = true,
                canPostNotifications = true,
                reportsChannelEnabled = false,
            ),
        )
    }

    @Test
    fun `expired Pro skips without changing the preference`() {
        assertEquals(
            WeeklyReportDeliveryDecision.PRO_INACTIVE,
            decideWeeklyReportDelivery(
                period = period,
                lastProcessedStart = null,
                lastProcessedEnd = null,
                weeklyEnabled = true,
                hasPro = false,
                notificationsEnabled = true,
                canPostNotifications = true,
                reportsChannelEnabled = true,
            ),
        )
    }
}
