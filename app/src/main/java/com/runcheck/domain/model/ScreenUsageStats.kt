package com.runcheck.domain.model

data class ScreenUsageStats(
    val screenOnDurationMs: Long,
    val screenOffDurationMs: Long,
    val screenOnDrainPct: Float,
    val screenOffDrainPct: Float,
    val screenOnDrainRate: Float?,
    val screenOffDrainRate: Float?,
)
