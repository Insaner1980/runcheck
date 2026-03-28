package com.runcheck.data.thermal

import android.os.Build
import android.os.PowerManager
import com.runcheck.domain.model.ThermalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalDataSourceHelpersTest {
    @Test
    fun `thermal status support starts at api 29`() {
        assertFalse(supportsThermalStatus(Build.VERSION_CODES.P))
        assertTrue(supportsThermalStatus(Build.VERSION_CODES.Q))
    }

    @Test
    fun `thermal headroom support starts at api 30`() {
        assertFalse(supportsThermalHeadroom(Build.VERSION_CODES.Q))
        assertTrue(supportsThermalHeadroom(Build.VERSION_CODES.R))
    }

    @Test
    fun `thermal status mapping returns none below api 29`() {
        assertEquals(
            ThermalStatus.NONE,
            mapThermalStatus(PowerManager.THERMAL_STATUS_SEVERE, Build.VERSION_CODES.P),
        )
    }

    @Test
    fun `thermal status mapping returns matching domain value on supported api`() {
        assertEquals(
            ThermalStatus.SEVERE,
            mapThermalStatus(PowerManager.THERMAL_STATUS_SEVERE, Build.VERSION_CODES.Q),
        )
        assertEquals(
            ThermalStatus.CRITICAL,
            mapThermalStatus(PowerManager.THERMAL_STATUS_CRITICAL, Build.VERSION_CODES.R),
        )
    }
}
