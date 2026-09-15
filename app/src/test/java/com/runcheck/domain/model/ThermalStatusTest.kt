package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ThermalStatusTest {
    @Test
    fun `active throttling starts at severe`() {
        val expected =
            listOf(
                ThermalStatus.NONE to false,
                ThermalStatus.LIGHT to false,
                ThermalStatus.MODERATE to false,
                ThermalStatus.SEVERE to true,
                ThermalStatus.CRITICAL to true,
                ThermalStatus.EMERGENCY to true,
                ThermalStatus.SHUTDOWN to true,
            )

        expected.forEach { (status, isThrottling) ->
            assertEquals(isThrottling, status.isThrottling)
        }
    }
}
