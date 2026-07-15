package com.runcheck.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSignalDbmTest {
    @Test
    fun `wifi uses capabilities RSSI dBm`() {
        assertEquals(
            -52,
            selectSignalDbmForTransport(
                isWifi = true,
                isCellular = false,
                capabilitiesWifiSignalDbm = -52,
                wifiSignalDbm = -61,
                cellularSignalDbm = null,
            ),
        )
    }

    @Test
    fun `wifi falls back when capabilities RSSI is invalid`() {
        assertEquals(
            -61,
            selectSignalDbmForTransport(
                isWifi = true,
                isCellular = false,
                capabilitiesWifiSignalDbm = -127,
                wifiSignalDbm = -61,
                cellularSignalDbm = null,
            ),
        )
    }

    @Test
    fun `cellular ignores bearer specific capabilities value`() {
        assertEquals(
            -104,
            selectSignalDbmForTransport(
                isWifi = false,
                isCellular = true,
                capabilitiesWifiSignalDbm = 3,
                wifiSignalDbm = null,
                cellularSignalDbm = -104,
            ),
        )
    }

    @Test
    fun `transport without a dBm source stays unavailable`() {
        assertNull(
            selectSignalDbmForTransport(
                isWifi = false,
                isCellular = false,
                capabilitiesWifiSignalDbm = 4,
                wifiSignalDbm = null,
                cellularSignalDbm = null,
            ),
        )
    }
}
