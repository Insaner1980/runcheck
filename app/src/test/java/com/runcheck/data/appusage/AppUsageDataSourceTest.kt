package com.runcheck.data.appusage

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.runcheck.util.TestAppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppUsageDataSourceTest {
    private val usageStatsManager = mockk<UsageStatsManager>()
    private val packageManager = mockk<PackageManager>(relaxed = true)
    private val context =
        mockk<Context> {
            every { getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
            every { getPackageManager() } returns this@AppUsageDataSourceTest.packageManager
        }
    private val dataSource = spyk(AppUsageDataSource(context, TestAppDispatchers()))

    @Test
    fun `daily bucket outside requested window does not identify a foreground app`() =
        runTest {
            every { dataSource.hasUsageStatsPermission() } returns true
            every { usageStatsManager.queryUsageStats(any(), any(), any()) } answers {
                listOf(usage("com.example.old", secondArg<Long>() - 1L))
            }

            assertNull(dataSource.getCurrentForegroundApp())
        }

    @Suppress("DEPRECATION")
    @Test
    fun `recent usage is selected while future bucket values are ignored`() =
        runTest {
            every { dataSource.hasUsageStatsPermission() } returns true
            every { packageManager.getApplicationLabel(any()) } returns "Recent app"
            every { usageStatsManager.queryUsageStats(any(), any(), any()) } answers {
                listOf(
                    usage("com.example.old", secondArg<Long>() - 1L),
                    usage("com.example.recent", secondArg()),
                    usage("com.example.future", thirdArg<Long>()),
                )
            }

            assertEquals("Recent app", dataSource.getCurrentForegroundApp())
            verify(exactly = 1) { packageManager.getApplicationInfo("com.example.recent", any<Int>()) }
        }

    private fun usage(
        packageName: String,
        lastTimeUsed: Long,
    ): UsageStats =
        mockk {
            every { getPackageName() } returns packageName
            every { getLastTimeUsed() } returns lastTimeUsed
        }
}
