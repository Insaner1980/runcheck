package com.runcheck.ui.battery

import com.runcheck.ui.chart.ChargingSessionSummary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryRemainingTimePanelVisibilityTest {
    @Test
    fun `remaining charge panel is hidden without pro access`() {
        assertFalse(
            shouldShowRemainingChargePanel(
                isPro = false,
                summary = chargingSummary(),
                currentLevel = 42,
            ),
        )
    }

    @Test
    fun `remaining charge panel is visible with pro access and meaningful estimate`() {
        assertTrue(
            shouldShowRemainingChargePanel(
                isPro = true,
                summary = chargingSummary(),
                currentLevel = 42,
            ),
        )
    }

    private fun chargingSummary() =
        ChargingSessionSummary(
            startLevel = 20,
            gainPercent = 22,
            durationMs = 60_000L,
            peakTemperatureC = 32f,
            averageCurrentMa = 1_500,
            deliveredMah = 25,
            averagePowerW = 8.5f,
            averageSpeedPctPerHour = 20f,
            recentSpeedPctPerHour = 18f,
            remainingTo80Ms = 120_000L,
            remainingTo100Ms = 240_000L,
            readings = emptyList(),
        )
}
