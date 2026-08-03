package com.runcheck.ui.battery

import com.runcheck.domain.model.BatteryReading

private const val MIN_DRAIN_RATE_WINDOW_MS = 10 * 60 * 1000

internal fun calculateDrainRate(history: List<BatteryReading>): Float? {
    if (history.size < 2) return null
    val recent = history.sortedByDescending { it.timestamp }
    val newest = recent.first()
    val oldest = recent.last()
    val timeDiffMs = newest.timestamp - oldest.timestamp
    if (timeDiffMs < MIN_DRAIN_RATE_WINDOW_MS) return null
    val levelDiff = oldest.level - newest.level
    if (levelDiff <= 0) return null
    val hours = timeDiffMs / (1000f * 60f * 60f)
    return levelDiff / hours
}
