package com.runcheck.domain.insights.analysis

import com.runcheck.domain.model.StorageReading
import javax.inject.Inject

class StorageGrowthAnalyzer
    @Inject
    constructor() {
        fun calculateFillRateBytesPerDay(readings: List<StorageReading>): Long? {
            if (readings.size < MIN_READINGS) return null

            val sorted = readings.sortedBy { it.timestamp }
            val usedValues = sorted.map { it.totalBytes - it.availableBytes }
            val times = sorted.map { it.timestamp.toDouble() }
            val n = sorted.size

            val sumX = times.sum()
            val sumY = usedValues.sumOf { it.toDouble() }
            val sumXY = times.zip(usedValues).sumOf { (x, y) -> x * y }
            val sumX2 = times.sumOf { it * it }

            val denominator = n * sumX2 - sumX * sumX
            if (denominator == 0.0) return null

            val slope = (n * sumXY - sumX * sumY) / denominator
            return (slope * DAY_MS).toLong()
        }

        fun formatEstimate(
            availableBytes: Long,
            bytesPerDay: Long,
        ): String? {
            if (bytesPerDay <= 0 || availableBytes <= 0) return null

            val days = availableBytes / bytesPerDay
            return when {
                days < 7 -> "${days}d"
                days < 60 -> "${days / 7}w"
                days < 730 -> "${days / 30}mo"
                else -> "${days / 365}y"
            }
        }

        companion object {
            private const val MIN_READINGS = 3
            private const val DAY_MS = 24L * 60L * 60L * 1000L
        }
    }
