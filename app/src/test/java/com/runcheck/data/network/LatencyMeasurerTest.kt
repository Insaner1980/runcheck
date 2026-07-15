package com.runcheck.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LatencyMeasurerTest {
    @Test
    fun `latency is the rounded average of successful samples`() {
        assertEquals(42, aggregateLatencySamples(listOf(20, 30, 40, 50, 68)))
    }

    @Test
    fun `latency is unavailable without successful samples`() {
        assertNull(aggregateLatencySamples(emptyList()))
    }
}
