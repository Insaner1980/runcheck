package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkSignalQualityTest {
    @Test
    fun `classifies 5g cellular signal with nr thresholds`() {
        assertCellularQuality("5G NR", -65, SignalQuality.EXCELLENT)
        assertCellularQuality("5G NR", -80, SignalQuality.GOOD)
        assertCellularQuality("5G NR", -90, SignalQuality.FAIR)
        assertCellularQuality("5G NR", -110, SignalQuality.POOR)
        assertCellularQuality("5G NR", -111, SignalQuality.NO_SIGNAL)
    }

    @Test
    fun `classifies lte cellular signal with lte thresholds`() {
        assertCellularQuality("4G LTE", -98, SignalQuality.EXCELLENT)
        assertCellularQuality("4G LTE", -108, SignalQuality.GOOD)
        assertCellularQuality("4G LTE", -118, SignalQuality.FAIR)
        assertCellularQuality("4G LTE", -128, SignalQuality.POOR)
        assertCellularQuality("4G LTE", -129, SignalQuality.NO_SIGNAL)
    }

    @Test
    fun `classifies wifi signal with wifi thresholds`() {
        assertEquals(SignalQuality.EXCELLENT, classifyNetworkSignalQuality(-49, ConnectionType.WIFI))
        assertEquals(SignalQuality.GOOD, classifyNetworkSignalQuality(-59, ConnectionType.WIFI))
        assertEquals(SignalQuality.FAIR, classifyNetworkSignalQuality(-69, ConnectionType.WIFI))
        assertEquals(SignalQuality.POOR, classifyNetworkSignalQuality(-79, ConnectionType.WIFI))
        assertEquals(SignalQuality.NO_SIGNAL, classifyNetworkSignalQuality(-80, ConnectionType.WIFI))
    }

    @Test
    fun `classifies vpn signal with vpn thresholds`() {
        assertEquals(SignalQuality.EXCELLENT, classifyNetworkSignalQuality(-79, ConnectionType.VPN))
        assertEquals(SignalQuality.GOOD, classifyNetworkSignalQuality(-89, ConnectionType.VPN))
        assertEquals(SignalQuality.FAIR, classifyNetworkSignalQuality(-99, ConnectionType.VPN))
        assertEquals(SignalQuality.POOR, classifyNetworkSignalQuality(-109, ConnectionType.VPN))
        assertEquals(SignalQuality.NO_SIGNAL, classifyNetworkSignalQuality(-110, ConnectionType.VPN))
    }

    @Test
    fun `missing or absent network signal is unavailable`() {
        assertEquals(SignalQuality.NO_SIGNAL, classifyNetworkSignalQuality(null, ConnectionType.WIFI))
        assertEquals(SignalQuality.NO_SIGNAL, classifyNetworkSignalQuality(-50, ConnectionType.NONE))
    }

    @Test
    fun `keeps vpn with unknown signal usable`() {
        assertEquals(
            SignalQuality.GOOD,
            classifyNetworkSignalQuality(
                dbm = null,
                type = ConnectionType.VPN,
            ),
        )
    }

    private fun assertCellularQuality(
        networkSubtype: String,
        dbm: Int,
        expected: SignalQuality,
    ) {
        assertEquals(
            expected,
            classifyNetworkSignalQuality(
                dbm = dbm,
                type = ConnectionType.CELLULAR,
                networkSubtype = networkSubtype,
            ),
        )
    }
}
