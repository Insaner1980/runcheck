package com.runcheck.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartDrawInRegistryTest {
    @Test
    fun datasetKeyAnimatesOnlyOnFirstPresentation() {
        val registry = ChartDrawInRegistry()

        assertTrue(registry.shouldAnimate("battery:LEVEL:DAY"))
        assertFalse(registry.shouldAnimate("battery:LEVEL:DAY"))
        assertTrue(registry.shouldAnimate("battery:CURRENT:DAY"))
        assertFalse(registry.shouldAnimate("battery:LEVEL:DAY"))
    }
}
