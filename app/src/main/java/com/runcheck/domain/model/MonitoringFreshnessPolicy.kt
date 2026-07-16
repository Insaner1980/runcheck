package com.runcheck.domain.model

object MonitoringFreshnessPolicy {
    fun staleAfterMillis(intervalMinutes: Int): Long = intervalMinutes * 60_000L * STALE_THRESHOLD_MULTIPLIER

    private const val STALE_THRESHOLD_MULTIPLIER = 3L
}
