package com.runcheck.domain.insights.analysis

import com.runcheck.domain.model.BatteryReading

internal fun List<BatteryReading>.dischargingPairs(
    dischargingStatuses: Set<String> = BatteryDrainAnalyzer.DEFAULT_DISCHARGING_STATUSES,
): List<Pair<BatteryReading, BatteryReading>> =
    sortedBy { it.timestamp }
        .zipWithNext()
        .filter { (_, current) -> current.status in dischargingStatuses }

internal fun Pair<BatteryReading, BatteryReading>.toDrainSample(): DrainSample? {
    val (previous, current) = this
    val levelDrop = (previous.level - current.level).coerceAtLeast(0)
    val durationMs = (current.timestamp - previous.timestamp).coerceAtLeast(0L)
    if (levelDrop <= 0 || durationMs <= 0L) return null

    return DrainSample(
        levelDropPct = levelDrop.toFloat(),
        durationMs = durationMs,
    )
}

internal fun List<Pair<BatteryReading, BatteryReading>>.toTimeIntervals(): List<TimeInterval> =
    map { (previous, current) ->
        TimeInterval(
            startTime = previous.timestamp,
            endTime = current.timestamp,
        )
    }
