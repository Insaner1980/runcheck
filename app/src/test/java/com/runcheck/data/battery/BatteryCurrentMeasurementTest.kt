package com.runcheck.data.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryCurrentMeasurementTest {
    @Test
    fun `microamps convert using integer division toward zero`() {
        val cases =
            mapOf(
                500_999 to 500,
                -500_999 to -500,
                999 to 0,
                -999 to 0,
                0 to 0,
                10_000_000 to 10_000,
                -10_000_000 to -10_000,
                10_000_999 to 10_000,
                -10_000_999 to -10_000,
                10_001_000 to 10_001,
                -10_001_000 to -10_001,
            )

        cases.forEach { (raw, expected) ->
            assertEquals("raw=$raw", expected, batteryCurrentMicroampsToMilliamps(raw))
        }
    }

    @Test
    fun `plausibility includes zero and both maximum magnitude boundaries`() {
        for (milliamps in listOf(0, 500, -500, 10_000, -10_000)) {
            assertTrue("milliamps=$milliamps", isPlausibleBatteryCurrent(milliamps))
        }
        for (milliamps in listOf(10_001, -10_001, Int.MIN_VALUE, Int.MAX_VALUE)) {
            assertFalse("milliamps=$milliamps", isPlausibleBatteryCurrent(milliamps))
        }
    }
}
