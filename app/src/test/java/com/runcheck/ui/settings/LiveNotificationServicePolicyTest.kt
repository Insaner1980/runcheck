package com.runcheck.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveNotificationServicePolicyTest {
    @Test
    fun `persisted enabled state starts a stopped service only when notifications are allowed`() {
        assertEquals(
            LiveNotificationServiceAction.START,
            resolveLiveNotificationServiceAction(
                enabled = true,
                canPostNotifications = true,
                isRunning = false,
            ),
        )
        assertEquals(
            LiveNotificationServiceAction.NONE,
            resolveLiveNotificationServiceAction(
                enabled = true,
                canPostNotifications = false,
                isRunning = false,
            ),
        )
    }

    @Test
    fun `latest disabled or permission denied state stops a running service`() {
        assertEquals(
            LiveNotificationServiceAction.STOP,
            resolveLiveNotificationServiceAction(
                enabled = false,
                canPostNotifications = true,
                isRunning = true,
            ),
        )
        assertEquals(
            LiveNotificationServiceAction.STOP,
            resolveLiveNotificationServiceAction(
                enabled = true,
                canPostNotifications = false,
                isRunning = true,
            ),
        )
    }
}
