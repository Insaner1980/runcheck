package com.runcheck.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionTypePersistenceTest {
    @Test
    fun `exact persisted names decode to their connection types`() {
        val cases =
            listOf(
                "WIFI" to ConnectionType.WIFI,
                "CELLULAR" to ConnectionType.CELLULAR,
                "ETHERNET" to ConnectionType.ETHERNET,
                "VPN" to ConnectionType.VPN,
                "NONE" to ConnectionType.NONE,
            )

        cases.forEach { (raw, expected) ->
            assertEquals(expected, decodePersistedConnectionType(raw))
        }
    }

    @Test
    fun `unknown noncanonical and blank persisted names decode to null`() {
        listOf("SATELLITE", "", "wifi", " WIFI ", "garbage", " ").forEach { raw ->
            assertNull(decodePersistedConnectionType(raw))
        }
    }
}
