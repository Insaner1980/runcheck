package com.runcheck.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringFreshnessPolicyTest {
    @Test
    fun `shorter interval does not make heartbeat from old longer schedule stale`() {
        val heartbeat = heartbeat(uptimeMillis = 1_000L, intervalMinutes = 60)

        assertFalse(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = heartbeat,
                currentIntervalMinutes = 15,
                currentUptimeMillis = 1_000L + minutes(46),
            ),
        )
    }

    @Test
    fun `heartbeat becomes stale after three old schedule intervals of awake time`() {
        val heartbeat = heartbeat(uptimeMillis = 1_000L, intervalMinutes = 60)

        assertTrue(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = heartbeat,
                currentIntervalMinutes = 15,
                currentUptimeMillis = 1_000L + minutes(180) + 1L,
            ),
        )
    }

    @Test
    fun `deep sleep gap does not contribute to stale age`() {
        val heartbeat = heartbeat(uptimeMillis = 1_000L, intervalMinutes = 15)

        assertFalse(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = heartbeat,
                currentIntervalMinutes = 15,
                currentUptimeMillis = 1_000L + minutes(20),
            ),
        )
    }

    @Test
    fun `continuous awake gap beyond threshold is stale`() {
        val heartbeat = heartbeat(uptimeMillis = 1_000L, intervalMinutes = 15)

        assertTrue(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = heartbeat,
                currentIntervalMinutes = 15,
                currentUptimeMillis = 1_000L + minutes(45) + 1L,
            ),
        )
    }

    @Test
    fun `heartbeat without uptime metadata is not treated as confirmed stale`() {
        assertFalse(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = null,
                currentIntervalMinutes = 15,
                currentUptimeMillis = minutes(60),
            ),
        )
    }

    @Test
    fun `heartbeat from a previous boot is not treated as confirmed stale`() {
        val heartbeat = heartbeat(uptimeMillis = minutes(60), intervalMinutes = 15)

        assertFalse(
            MonitoringFreshnessPolicy.isStale(
                heartbeat = heartbeat,
                currentIntervalMinutes = 15,
                currentUptimeMillis = minutes(5),
            ),
        )
    }

    private fun heartbeat(
        uptimeMillis: Long,
        intervalMinutes: Int,
    ) = MonitoringHeartbeat(
        recordedAtEpochMillis = 1_000L,
        recordedAtUptimeMillis = uptimeMillis,
        intervalMinutes = intervalMinutes,
    )

    private fun minutes(value: Int): Long = value * 60_000L
}
