package com.runcheck.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkCallbackRegistrationTest {
    @Test
    fun `unregisters receiver when callback registration fails`() {
        var receiverRegistrations = 0
        var receiverUnregistrations = 0

        assertThrows(IllegalStateException::class.java) {
            registerCallbackWithReceiverRollback(
                registerReceiver = { receiverRegistrations++ },
                registerCallback = { error("callback registration failed") },
                unregisterReceiver = { receiverUnregistrations++ },
            )
        }

        assertEquals(1, receiverRegistrations)
        assertEquals(1, receiverUnregistrations)
    }
}
