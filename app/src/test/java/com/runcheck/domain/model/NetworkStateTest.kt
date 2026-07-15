package com.runcheck.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStateTest {
    @Test
    fun `disconnected network is not connected`() {
        assertFalse(networkState(ConnectionType.NONE).isConnected)
    }

    @Test
    fun `available connection types are connected`() {
        assertTrue(networkState(ConnectionType.WIFI).isConnected)
        assertTrue(networkState(ConnectionType.CELLULAR).isConnected)
        assertTrue(networkState(ConnectionType.VPN).isConnected)
    }

    private fun networkState(connectionType: ConnectionType) =
        NetworkState(
            connectionType = connectionType,
            signalDbm = null,
            signalQuality = SignalQuality.NO_SIGNAL,
        )
}
