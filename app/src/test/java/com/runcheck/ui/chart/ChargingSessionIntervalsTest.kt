package com.runcheck.ui.chart

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.ChargingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ChargingSessionIntervalsTest {
    @Test
    fun `valid intervals use trapezoidal current and total accepted duration`() {
        assertCurrents(listOf(0L to 600, 600_000L to 1_800, 1_800_000L to 600), 600, 1_200)
    }

    @Test
    fun `missing current at either endpoint rejects the interval`() {
        assertCurrents(listOf(0L to null, 600_000L to 1_200), null, null)
        assertCurrents(listOf(0L to 1_200, 600_000L to null), null, null)
    }

    @Test
    fun `duplicate timestamps have no valid duration`() {
        assertCurrents(listOf(0L to 600, 0L to 1_800), null, null)
    }

    @Test
    fun `out of order input is sorted before intervals are evaluated`() {
        assertCurrents(listOf(600_000L to 1_800, 0L to 600, 600_000L to 600), 200, 1_200)
    }

    @Test
    fun `negative duration from timestamp overflow is rejected`() {
        assertCurrents(listOf(Long.MIN_VALUE to 1_200, Long.MAX_VALUE to 1_200), null, null)
    }

    @Test
    fun `maximum sample gap is accepted but one millisecond more is rejected`() {
        assertCurrents(listOf(0L to 1_200, MAX_SESSION_SAMPLE_GAP_MS to 1_200), 600, 1_200)
        assertCurrents(listOf(0L to 1_200, (MAX_SESSION_SAMPLE_GAP_MS + 1) to 1_200), null, null)
    }

    @Test
    fun `interval average is clamped after adding signed endpoint currents`() {
        assertCurrents(listOf(0L to -1_200, 600_000L to -600), 0, 0)
        assertCurrents(listOf(0L to -600, 600_000L to 1_800), 100, 600)
        assertCurrents(listOf(0L to -1_800, 600_000L to 600), 0, 0)
    }

    @Test
    fun `delivered charge and denominator use only the same accepted intervals`() {
        // Accepted durations: 10 minutes at 1200 mA, 10 at 0 mA, 30 at 600 mA.
        assertCurrents(
            listOf(
                0L to 1_200,
                600_000L to 1_200,
                1_200_000L to null,
                1_800_000L to -600,
                2_400_000L to -600,
                2_400_000L to 600,
                4_200_001L to 600,
                6_000_001L to 600,
            ),
            500,
            600,
        )
    }

    @Test
    fun `single sample and entirely missing currents have no valid intervals`() {
        assertCurrents(listOf(0L to 1_200), null, null)
        assertCurrents(listOf(0L to null, 600_000L to null, 1_200_000L to null), null, null)
    }

    @Test
    fun `average current uses delivered charge rounded once after accumulation`() {
        // Each interval delivers 0.4 mAh; 0.8 rounds to 1, giving 75 mA rather than 60.
        assertCurrents(listOf(0L to 60, 24_000L to 60, 48_000L to 60), 1, 75)
        // A valid interval whose delivered charge rounds to zero still has a zero average.
        assertCurrents(listOf(0L to 60, 24_000L to 60), 0, 0)
    }

    private fun assertCurrents(
        samples: List<Pair<Long, Int?>>,
        deliveredMah: Int?,
        averageCurrentMa: Int?,
    ) {
        val summary =
            calculateChargingSessionSummary(
                history = samples.map { (timestamp, current) -> reading(timestamp, current) },
                currentLevel = 50,
                chargingStatus = ChargingStatus.CHARGING,
            )
        assertNotNull(summary)
        assertEquals(deliveredMah, summary?.deliveredMah)
        assertEquals(averageCurrentMa, summary?.averageCurrentMa)
    }

    private fun reading(
        timestamp: Long,
        current: Int?,
    ) = BatteryReading(
        timestamp = timestamp,
        level = 50,
        voltageMv = 4_000,
        temperatureC = 30f,
        currentMa = current,
        currentConfidence = "HIGH",
        status = ChargingStatus.CHARGING.name,
        plugType = "USB",
        health = "GOOD",
        cycleCount = null,
        healthPct = null,
    )
}
