package com.runcheck.pro

import com.runcheck.worker.TrialNotificationWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class TrialManagerTest {
    @Test
    fun `first launch starts full trial at current timestamp`() {
        val now = TimeUnit.DAYS.toMillis(20)

        val resolution =
            TrialManager.resolveTrialState(
                startTimestamp = 0L,
                lastKnownTimestamp = 0L,
                storedClockTampered = false,
                now = now,
            )

        assertFalse(resolution.clockTampered)
        assertTrue(resolution.state.isActive)
        assertTrue(resolution.state.isFirstLaunch)
        assertEquals(TrialManager.TRIAL_DURATION_DAYS, resolution.state.daysRemaining)
        assertEquals(now, resolution.state.startTimestamp)
        assertEquals(now, resolution.lastKnownTimestamp)
    }

    @Test
    fun `active existing trial reports remaining days from elapsed whole days`() {
        val now = TimeUnit.DAYS.toMillis(20)
        val startTimestamp = now - TimeUnit.DAYS.toMillis(3)

        val resolution =
            TrialManager.resolveTrialState(
                startTimestamp = startTimestamp,
                lastKnownTimestamp = now - TimeUnit.HOURS.toMillis(2),
                storedClockTampered = false,
                now = now,
            )

        assertFalse(resolution.clockTampered)
        assertTrue(resolution.state.isActive)
        assertFalse(resolution.state.isFirstLaunch)
        assertEquals(4, resolution.state.daysRemaining)
        assertEquals(startTimestamp, resolution.state.startTimestamp)
        assertEquals(now, resolution.lastKnownTimestamp)
    }

    @Test
    fun `stored clock tamper flag keeps trial inactive even when current clock moves forward`() {
        val now = TimeUnit.DAYS.toMillis(20)
        val startTimestamp = now - TimeUnit.DAYS.toMillis(1)

        val resolution =
            TrialManager.resolveTrialState(
                startTimestamp = startTimestamp,
                lastKnownTimestamp = now - TimeUnit.HOURS.toMillis(1),
                storedClockTampered = true,
                now = now,
            )

        assertTrue(resolution.clockTampered)
        assertFalse(resolution.state.isActive)
        assertEquals(0, resolution.state.daysRemaining)
        assertEquals(now, resolution.lastKnownTimestamp)
    }

    @Test
    fun `clock rollback is sticky and does not move last known timestamp backwards`() {
        val lastKnown = TimeUnit.DAYS.toMillis(10)
        val rolledBackNow = lastKnown - TimeUnit.HOURS.toMillis(2)
        val startTimestamp = lastKnown - TimeUnit.DAYS.toMillis(1)

        val firstResolution =
            TrialManager.resolveTrialState(
                startTimestamp = startTimestamp,
                lastKnownTimestamp = lastKnown,
                storedClockTampered = false,
                now = rolledBackNow,
            )

        assertTrue(firstResolution.clockTampered)
        assertFalse(firstResolution.state.isActive)
        assertEquals(0, firstResolution.state.daysRemaining)
        assertEquals(lastKnown, firstResolution.lastKnownTimestamp)

        val secondResolution =
            TrialManager.resolveTrialState(
                startTimestamp = startTimestamp,
                lastKnownTimestamp = firstResolution.lastKnownTimestamp,
                storedClockTampered = firstResolution.clockTampered,
                now = rolledBackNow,
            )

        assertTrue(secondResolution.clockTampered)
        assertFalse(secondResolution.state.isActive)
        assertEquals(0, secondResolution.state.daysRemaining)
        assertEquals(lastKnown, secondResolution.lastKnownTimestamp)
    }

    @Test
    fun `future trial start from local clock drift clamps remaining days to trial duration`() {
        val now = TimeUnit.DAYS.toMillis(2)
        val futureStart = now + TimeUnit.DAYS.toMillis(3)

        val resolution =
            TrialManager.resolveTrialState(
                startTimestamp = futureStart,
                lastKnownTimestamp = now,
                storedClockTampered = false,
                now = now,
            )

        assertFalse(resolution.clockTampered)
        assertTrue(resolution.state.isActive)
        assertEquals(TrialManager.TRIAL_DURATION_DAYS, resolution.state.daysRemaining)
        assertEquals(now, resolution.lastKnownTimestamp)
    }

    @Test
    fun `expired trial clamps remaining days to zero`() {
        val now = TimeUnit.DAYS.toMillis(10)
        val startTimestamp = now - TimeUnit.DAYS.toMillis(8)

        val resolution =
            TrialManager.resolveTrialState(
                startTimestamp = startTimestamp,
                lastKnownTimestamp = now,
                storedClockTampered = false,
                now = now,
            )

        assertFalse(resolution.state.isActive)
        assertEquals(0, resolution.state.daysRemaining)
    }

    @Test
    fun `trial notification work names and tags expose separate roles`() {
        assertEquals("trial_notification_day5", TrialNotificationWorker.UNIQUE_WORK_DAY5)
        assertEquals("trial_notification_day7", TrialNotificationWorker.UNIQUE_WORK_DAY7)
        assertEquals("trial_notification_day5", TrialNotificationWorker.TAG_DAY5)
        assertEquals("trial_notification_day7", TrialNotificationWorker.TAG_DAY7)
    }
}
