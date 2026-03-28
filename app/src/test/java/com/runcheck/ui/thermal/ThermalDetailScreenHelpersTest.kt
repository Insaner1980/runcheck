package com.runcheck.ui.thermal

import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalDetailScreenHelpersTest {
    @Test
    fun `neutral thermal status is used when none is the only available signal`() {
        val thermal =
            ThermalState(
                batteryTempC = 31f,
                cpuTempC = null,
                thermalHeadroom = null,
                thermalStatus = ThermalStatus.NONE,
                isThrottling = false,
            )

        assertTrue(shouldUseNeutralThermalStatus(thermal))
    }

    @Test
    fun `neutral thermal status is not used when headroom corroborates normal state`() {
        val thermal =
            ThermalState(
                batteryTempC = 31f,
                cpuTempC = null,
                thermalHeadroom = 0.42f,
                thermalStatus = ThermalStatus.NONE,
                isThrottling = false,
            )

        assertFalse(shouldUseNeutralThermalStatus(thermal))
    }

    @Test
    fun `neutral thermal status is not used while throttling is active`() {
        val thermal =
            ThermalState(
                batteryTempC = 44f,
                cpuTempC = null,
                thermalHeadroom = null,
                thermalStatus = ThermalStatus.SEVERE,
                isThrottling = true,
            )

        assertFalse(shouldUseNeutralThermalStatus(thermal))
    }
}
