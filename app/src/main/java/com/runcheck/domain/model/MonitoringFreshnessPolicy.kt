package com.runcheck.domain.model

object MonitoringFreshnessPolicy {
    fun staleAfterMillis(intervalMinutes: Int): Long = intervalMinutes * 60_000L * STALE_THRESHOLD_MULTIPLIER

    fun isStale(
        heartbeat: MonitoringHeartbeat?,
        currentIntervalMinutes: Int,
        currentUptimeMillis: Long,
        currentEpochMillis: Long,
    ): Boolean {
        val recordedUptimeMillis = heartbeat?.recordedAtUptimeMillis ?: return false
        val effectiveIntervalMinutes =
            maxOf(currentIntervalMinutes, heartbeat.intervalMinutes)
        val elapsedMillis =
            if (currentUptimeMillis >= recordedUptimeMillis) {
                currentUptimeMillis - recordedUptimeMillis
            } else {
                currentEpochMillis - heartbeat.recordedAtEpochMillis
            }
        return elapsedMillis >= 0L && elapsedMillis > staleAfterMillis(effectiveIntervalMinutes)
    }

    private const val STALE_THRESHOLD_MULTIPLIER = 3L
}
