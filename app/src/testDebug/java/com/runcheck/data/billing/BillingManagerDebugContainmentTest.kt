package com.runcheck.data.billing

import android.content.Context
import com.runcheck.util.AppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillingManagerDebugContainmentTest {
    @Test
    fun `debug Pro override does not persist purchase entitlement`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val dispatchers = mockk<AppDispatchers>()
            val proStatusCache = mockk<ProStatusCache>(relaxed = true)
            every { dispatchers.main } returns dispatcher
            val manager =
                BillingManager(
                    context = mockk<Context>(relaxed = true),
                    proStatusCache = proStatusCache,
                    dispatchers = dispatchers,
                )

            manager.initialize()

            assertTrue(manager.isPro())
            verify(exactly = 0) { proStatusCache.setCachedProStatus(any()) }
            manager.destroy()
        }
}
