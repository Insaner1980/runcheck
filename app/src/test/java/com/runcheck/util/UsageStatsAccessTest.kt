package com.runcheck.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UsageStatsAccessTest(
    private val mode: Int?,
    private val expectedAccess: Boolean,
) {
    @Before
    fun setUp() {
        mockkStatic(Process::class)
        every { Process.myUid() } returns 12345
    }

    @After
    fun tearDown() {
        unmockkStatic(Process::class)
    }

    @Test
    fun `only allowed grants access using the current app identity`() {
        val appOps = mockk<AppOpsManager>()
        val context =
            mockk<Context> {
                every { getSystemService(Context.APP_OPS_SERVICE) } returns if (mode == null) null else appOps
                every { packageName } returns "com.runcheck"
            }
        if (mode != null) {
            every { appOps.checkOpNoThrow(any<String>(), any(), any()) } returns mode
        }

        assertEquals(expectedAccess, context.hasUsageStatsAccess())

        verify(exactly = if (mode == null) 0 else 1) {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, 12345, "com.runcheck")
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "mode={0}, access={1}")
        fun modes(): List<Array<Any?>> =
            listOf(
                arrayOf(AppOpsManager.MODE_ALLOWED, true),
                arrayOf(AppOpsManager.MODE_IGNORED, false),
                arrayOf(AppOpsManager.MODE_ERRORED, false),
                arrayOf(AppOpsManager.MODE_DEFAULT, false),
                arrayOf(AppOpsManager.MODE_FOREGROUND, false),
                arrayOf(null, false),
            )
    }
}
