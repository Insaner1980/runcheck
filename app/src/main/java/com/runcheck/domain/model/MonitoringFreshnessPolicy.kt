package com.runcheck.domain.model

object MonitoringFreshnessPolicy {
    fun staleAfterMillis(intervalMinutes: Int): Long = intervalMinutes * 60_000L * STALE_THRESHOLD_MULTIPLIER

    fun isStale(
        heartbeat: MonitoringHeartbeat?,
        currentIntervalMinutes: Int,
        currentUptimeMillis: Long,
    ): Boolean {
        val recordedUptimeMillis = heartbeat?.recordedAtUptimeMillis ?: return false
        if (currentUptimeMillis < recordedUptimeMillis) return false

        val effectiveIntervalMinutes =
            maxOf(currentIntervalMinutes, heartbeat.intervalMinutes)
        return currentUptimeMillis - recordedUptimeMillis > staleAfterMillis(effectiveIntervalMinutes)
    }

    private const val STALE_THRESHOLD_MULTIPLIER = 3L
}
