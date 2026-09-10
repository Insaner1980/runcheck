package com.runcheck.data.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryCycleCountTest {
    @Test
    fun `zero is a valid reported cycle count`() {
        assertEquals(0, normalizeCycleCount(0))
        assertEquals(10_000, normalizeCycleCount(10_000))
    }

    @Test
    fun `missing and implausible cycle counts are unavailable`() {
        assertNull(normalizeCycleCount(-1))
        assertNull(normalizeCycleCount(10_001))
    }
}
