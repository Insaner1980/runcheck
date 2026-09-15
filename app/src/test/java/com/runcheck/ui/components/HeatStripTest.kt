package com.runcheck.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatStripTest {
    @Test
    fun `fixed pulse starts strictly above 42 Celsius`() {
        assertFalse(shouldPulseHeatStrip(Math.nextDown(42f), false))
        assertFalse(shouldPulseHeatStrip(42f, false))
        assertTrue(shouldPulseHeatStrip(Math.nextUp(42f), false))
        assertTrue(shouldPulseHeatStrip(43f, false))
    }

    @Test
    fun `reduced motion always disables pulse`() {
        listOf(35f, 42f, Math.nextUp(42f), 45f, 60f).forEach {
            assertFalse(shouldPulseHeatStrip(it, true))
        }
    }
}
