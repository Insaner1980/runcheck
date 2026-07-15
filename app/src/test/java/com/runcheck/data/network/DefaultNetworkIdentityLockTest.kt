package com.runcheck.data.network

import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultNetworkIdentityLockTest {
    private val lockedNetwork = mockk<Network>()
    private val replacementNetwork = mockk<Network>()

    @Test
    fun `wifi to cellular default network handover fails the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork)

        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForAvailable(replacementNetwork),
        )
    }

    @Test
    fun `changed current default network is caught before the test starts`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork)

        assertNull(lock.failureForCurrentDefault(lockedNetwork))
        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForCurrentDefault(replacementNetwork),
        )
        assertEquals(NetworkLockFailure.NO_INTERNET, lock.failureForCurrentDefault(null))
    }

    @Test
    fun `benign capability update on locked network does not fail the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork)
        val capabilities = validatedCapabilities()

        assertNull(lock.failureForCapabilities(lockedNetwork, capabilities))
    }

    @Test
    fun `capabilities from replacement default network fail the lock`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork)

        assertEquals(
            NetworkLockFailure.CONNECTION_CHANGED,
            lock.failureForCapabilities(replacementNetwork, validatedCapabilities()),
        )
    }

    @Test
    fun `loss of validation on locked network fails as no internet`() {
        val lock = DefaultNetworkIdentityLock(lockedNetwork)
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
        val lock = DefaultNetworkIdentityLock(lockedNetwork)

        assertEquals(NetworkLockFailure.NO_INTERNET, lock.failureForLost(lockedNetwork))
        assertNull(lock.failureForLost(replacementNetwork))
    }

    private fun validatedCapabilities(): NetworkCapabilities =
        mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
        }
}
