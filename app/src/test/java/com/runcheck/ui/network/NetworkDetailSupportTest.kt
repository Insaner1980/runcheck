package com.runcheck.ui.network

import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkDetailSupportTest {
    @Test
    fun `groupIpAddresses preserves every IPv4 and IPv6 address`() {
        val addresses =
            listOf(
                "192.0.2.10",
                "2001:db8::10",
                "198.51.100.20",
                "fe80::20%wlan0",
            )

        val (ipv4Addresses, ipv6Addresses) = groupIpAddresses(addresses)

        assertEquals(listOf("192.0.2.10", "198.51.100.20"), ipv4Addresses)
        assertEquals(listOf("2001:db8::10", "fe80::20%wlan0"), ipv6Addresses)
    }
}
