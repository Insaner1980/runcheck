package com.runcheck.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NetworkSignalDbmTest {
    @Test
    fun `cellular ASU unknown values cannot replace a valid signal`() {
        val values = listOf(Int.MIN_VALUE, -1, 40, 99, 255, Int.MAX_VALUE)

        assertEquals(40, values.mapNotNull(::normalizeCellularSignalAsu).maxOrNull())
        assertNull(normalizeCellularSignalAsu(99))
        assertNull(normalizeCellularSignalAsu(255))
        assertEquals(0, normalizeCellularSignalAsu(0))
        assertEquals(97, normalizeCellularSignalAsu(97))
    }

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
    fun `wifi falls back when capabilities RSSI is positive`() {
        assertEquals(
            -61,
            selectSignalDbmForTransport(
                isWifi = true,
                isCellular = false,
                capabilitiesWifiSignalDbm = 1,
                wifiSignalDbm = -61,
                cellularSignalDbm = null,
            ),
        )
    }

    @Test
    fun `unknown and nonpositive wifi link metrics stay unavailable`() {
        assertNull(normalizePositiveWifiMetric(-1))
        assertNull(normalizePositiveWifiMetric(0))
        assertEquals(866, normalizePositiveWifiMetric(866))
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
