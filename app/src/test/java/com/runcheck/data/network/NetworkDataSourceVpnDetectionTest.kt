package com.runcheck.data.network

import com.runcheck.domain.model.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkDataSourceVpnDetectionTest {
    @Test
    fun `maps an ethernet transport as a connected ethernet network`() {
        assertEquals(
            ConnectionType.ETHERNET,
            resolveConnectionType(
                isWifi = false,
                isCellular = false,
                isEthernet = true,
                isVpn = false,
            ),
        )
    }

    @Test
    fun `returns true when vpn transport exists and network is not marked not-vpn`() {
        assertTrue(
            resolveVpnState(
                hasVpnTransport = true,
                hasNotVpnCapability = false,
            ),
        )
    }

    @Test
    fun `returns false when vpn transport is absent`() {
        assertFalse(
            resolveVpnState(
                hasVpnTransport = false,
                hasNotVpnCapability = false,
            ),
        )
    }

    @Test
    fun `returns false when device reports vpn transport but network is still marked not-vpn`() {
        assertFalse(
            resolveVpnState(
                hasVpnTransport = true,
                hasNotVpnCapability = true,
            ),
        )
    }
}
