package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChargingSessionPowerTest {
    @Test
    fun `reconstruction is unavailable when both averages are missing`() {
        assertNull(session(avgCurrentMa = null, avgVoltageMv = null).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction is unavailable when average current is missing`() {
        assertNull(session(avgCurrentMa = null, avgVoltageMv = 5_000).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction is unavailable when average voltage is missing`() {
        assertNull(session(avgCurrentMa = 2_000, avgVoltageMv = null).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction multiplies average current and voltage`() {
        assertEquals(10_000, session(avgCurrentMa = 2_000, avgVoltageMv = 5_000).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction preserves truncating integer division`() {
        assertEquals(4, session(avgCurrentMa = 1, avgVoltageMv = 4_500).reconstructedAveragePowerMw())
        assertEquals(10_007, session(avgCurrentMa = 2_001, avgVoltageMv = 5_001).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction preserves zero`() {
        assertEquals(0, session(avgCurrentMa = 0, avgVoltageMv = 5_000).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction preserves representable negative power`() {
        assertEquals(-10_000, session(avgCurrentMa = -2_000, avgVoltageMv = 5_000).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction widens before multiplying`() {
        assertEquals(2_147_490, session(avgCurrentMa = 10_000, avgVoltageMv = 214_749).reconstructedAveragePowerMw())
    }

    @Test
    fun `reconstruction is unavailable above Int range`() {
        assertNull(
            session(avgCurrentMa = Int.MAX_VALUE, avgVoltageMv = 1_001).reconstructedAveragePowerMw(),
        )
    }

    @Test
    fun `reconstruction is unavailable below Int range`() {
        assertNull(
            session(avgCurrentMa = Int.MIN_VALUE, avgVoltageMv = 1_001).reconstructedAveragePowerMw(),
        )
    }

    private fun session(
        avgCurrentMa: Int?,
        avgVoltageMv: Int?,
    ) = ChargingSession(
        chargerId = 1L,
        startTime = 1_000L,
        endTime = 2_000L,
        startLevel = 20,
        endLevel = 80,
        avgCurrentMa = avgCurrentMa,
        maxCurrentMa = null,
        avgVoltageMv = avgVoltageMv,
        avgPowerMw = null,
        plugType = "USB",
    )
}
