package com.runcheck.ui.appusage

import com.runcheck.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppUsageCopyTest {
    @Test
    fun `reachable App Usage surfaces use truthful feature naming`() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals("App Usage", context.getString(R.string.app_usage_title))
        assertEquals("App usage", context.getString(R.string.pro_feature_app_usage))
    }
}
