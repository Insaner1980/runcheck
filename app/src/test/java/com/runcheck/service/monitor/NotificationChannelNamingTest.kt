package com.runcheck.service.monitor

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationChannelNamingTest {
    @Test
    fun `real time notification channel id is owned by notification helper and stays stable`() {
        assertEquals("real_time_monitor", NotificationHelper.CHANNEL_REAL_TIME)
        assertEquals(NotificationHelper.CHANNEL_REAL_TIME, RealTimeMonitorService.CHANNEL_ID)
    }
}
