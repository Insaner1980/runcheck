package com.runcheck.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.runcheck.R
import com.runcheck.domain.model.SpeedTestProgress
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SpeedTestConnectionPrecheckTest {
    @Test
    fun `unvalidated active network has a distinct error from no connection`() =
        runTest {
            val noConnectionError = precheckFailure(activeNetwork = null, capabilities = null)
            val activeNetwork = mockk<Network>()
            val unvalidatedCapabilities =
                mockk<NetworkCapabilities> {
                    every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
                    every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false
                }

            val unvalidatedError = precheckFailure(activeNetwork, unvalidatedCapabilities)

            assertEquals(R.string.speed_test_error_no_internet.toString(), noConnectionError)
            assertEquals(R.string.speed_test_error_connection_not_validated.toString(), unvalidatedError)
            assertNotEquals(noConnectionError, unvalidatedError)
        }

    private suspend fun precheckFailure(
        activeNetwork: Network?,
        capabilities: NetworkCapabilities?,
    ): String {
        val connectivityManager = mockk<ConnectivityManager>()
        val context =
            mockk<Context> {
                every { getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
                every { getString(any()) } answers { firstArg<Int>().toString() }
            }
        every { connectivityManager.activeNetwork } returns activeNetwork
        if (activeNetwork != null) {
            every { connectivityManager.getNetworkCapabilities(activeNetwork) } returns capabilities
        }
        val service =
            SpeedTestService(
                context = context,
                latencyMeasurer = mockk(relaxed = true),
                networkDataSource = mockk(relaxed = true),
            )

        return (service.runSpeedTest().first() as SpeedTestProgress.Failed).error
    }
}
