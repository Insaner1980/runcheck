package com.runcheck.domain.usecase

import androidx.annotation.CheckResult
import com.runcheck.domain.model.StorageReading
import javax.inject.Inject

class CalculateFillRateUseCase @Inject constructor() {

    /**
     * Calculates storage fill rate in bytes/day using linear regression
     * over historical readings. Returns null if insufficient data.
     */
    @CheckResult
    operator fun invoke(readings: List<StorageReading>): Long? {
        if (readings.size < MIN_READINGS) return null

        val n = readings.size
        val usedValues = readings.map { it.totalBytes - it.availableBytes }
        val times = readings.map { it.timestamp.toDouble() }

        val sumX = times.sum()
        val sumY = usedValues.sumOf { it.toDouble() }
        val sumXY = times.zip(usedValues).sumOf { (x, y) -> x * y }
        val sumX2 = times.sumOf { it * it }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0.0) return null

        val slope = (n * sumXY - sumX * sumY) / denominator // bytes per ms
        return (slope * DAY_MS).toLong()
    }

    /**
     * Formats an estimated time until storage is full.
     * Returns null if the fill rate is zero or negative (not filling up).
     */
    @CheckResult
    fun formatEstimate(availableBytes: Long, bytesPerDay: Long): String? {
        if (bytesPerDay <= 0 || availableBytes <= 0) return null
        val days = availableBytes / bytesPerDay
        return when {
            days < 7 -> "${days}d"
            days < 60 -> "${days / 7}w"
            days < 730 -> "${days / 30}mo"
            else -> "${days / 365}y"
        }
    }

    private companion object {
        const val MIN_READINGS = 3
        const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
