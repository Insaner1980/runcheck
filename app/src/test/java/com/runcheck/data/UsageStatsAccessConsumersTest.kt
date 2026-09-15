package com.runcheck.data

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.runcheck.data.appusage.AppUsageDataSource
import com.runcheck.data.storage.MediaStoreScanner
import com.runcheck.data.storage.StorageDataSource
import com.runcheck.util.TestAppDispatchers
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UsageStatsAccessConsumersTest(
    private val managerAvailable: Boolean,
) {
    private val usageStatsManager = mockk<UsageStatsManager>()
    private val storageStatsManager = mockk<StorageStatsManager>()
    private val appOps = mockk<AppOpsManager>()
    private val context =
        mockk<Context> {
            every { getSystemService(Context.USAGE_STATS_SERVICE) } returns usageStatsManager
            every { getSystemService(Context.STORAGE_STATS_SERVICE) } returns storageStatsManager
            every { getSystemService(Context.STORAGE_SERVICE) } returns null
            every { getSystemService(Context.DEVICE_POLICY_SERVICE) } returns null
            every { getSystemService(Context.APP_OPS_SERVICE) } returns if (managerAvailable) appOps else null
            every { packageName } returns "com.runcheck"
        }

    @Before
    fun setUp() {
        mockkStatic(Process::class)
        every { Process.myUid() } returns 12345
        every { appOps.checkOpNoThrow(any<String>(), any(), any()) } returns AppOpsManager.MODE_IGNORED
    }

    @After
    fun tearDown() {
        unmockkStatic(Process::class)
    }

    @Test
    fun `app usage reports missing access without collecting usage`() =
        runTest {
            val source = AppUsageDataSource(context, TestAppDispatchers())

            assertFalse(source.hasUsageStatsPermission())
            assertNull(source.getUsageSince(1L, 2L))
            assertNull(source.getCurrentForegroundApp())

            verify { usageStatsManager wasNot Called }
        }

    @Test
    fun `storage preserves capacity but leaves aggregate app statistics unavailable`() =
        runTest {
            every { storageStatsManager.getTotalBytes(any()) } returns 1000L
            every { storageStatsManager.getFreeBytes(any()) } returns 400L
            val source =
                StorageDataSource(
                    context,
                    mockk<MediaStoreScanner>(relaxed = true),
                    TestAppDispatchers(),
                )

            assertFalse(source.hasUsageStatsPermission())
            val info = source.getStorageInfo()

            assertEquals(1000L, info.totalBytes)
            assertEquals(400L, info.availableBytes)
            assertEquals(600L, info.usedBytes)
            assertNull(info.appsBytes)
            assertNull(info.totalCacheBytes)
            verify(exactly = 0) { storageStatsManager.queryStatsForUser(any(), any()) }
        }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "managerAvailable={0}")
        fun availability(): List<Array<Boolean>> = listOf(arrayOf(true), arrayOf(false))
    }
}
