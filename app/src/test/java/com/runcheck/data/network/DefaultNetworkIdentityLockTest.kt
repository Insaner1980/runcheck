package com.runcheck.data.network

import android.net.Network
import android.net.NetworkCapabilities
import com.runcheck.domain.model.ConnectionType
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultNetworkIdentityLockTest {
    private val lockedNetwork = mockk<Network>()
    private val replacementNetwork = mockk<Network>()

    @Test
    fun `vpn bearer change on the same network fails the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)
        val capabilities = validatedCapabilities()
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) } returns true
        assertNull(lock.failureForCapabilities(lockedNetwork, capabilities))

        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForCapabilities(lockedNetwork, capabilities),
        )
    }

    @Test
    fun `wifi to cellular default network handover fails the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)

        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForAvailable(replacementNetwork),
        )
    }

    @Test
    fun `changed current default network is caught before the test starts`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)

        assertNull(lock.failureForCurrentDefault(lockedNetwork))
        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForCurrentDefault(replacementNetwork),
        )
        assertEquals(NetworkLockFailure.NO_INTERNET, lock.failureForCurrentDefault(null))
    }

    @Test
    fun `benign capability update on locked network does not fail the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)
        val capabilities = validatedCapabilities()

        assertNull(lock.failureForCapabilities(lockedNetwork, capabilities))
    }

    @Test
    fun `capabilities from replacement default network fail the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)

        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForCapabilities(replacementNetwork, validatedCapabilities()),
        )
    }

    @Test
    fun `loss of validation on locked network fails as no internet`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)
        val capabilities =
            mockk<NetworkCapabilities> {
                every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
                every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false
            }

        assertEquals(
            NetworkLockFailure.NO_INTERNET,
            lock.failureForCapabilities(lockedNetwork, capabilities),
        )
    }

    @Test
    fun `loss of locked network fails as no internet`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork, ConnectionType.WIFI)

        assertEquals(NetworkLockFailure.NO_INTERNET, lock.failureForLost(lockedNetwork))
        assertNull(lock.failureForLost(replacementNetwork))
    }

    private fun validatedCapabilities(): NetworkCapabilities =
        mockk {
            every { hasTransport(any()) } answers { firstArg<Int>() == NetworkCapabilities.TRANSPORT_WIFI }
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN) } returns false
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        }
}
