package com.runcheck.domain.usecase

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.StorageReading
import javax.inject.Inject

class CalculateFillRateUseCase
    @Inject
    constructor(
        private val storageGrowthAnalyzer: StorageGrowthAnalyzer,
    ) {
        /**
         * Calculates storage fill rate in bytes/day using linear regression
         * over historical readings. Returns null if insufficient data.
         */
        operator fun invoke(readings: List<StorageReading>): Long? =
            storageGrowthAnalyzer.calculateFillRateBytesPerDay(readings)

        /**
         * Formats an estimated time until storage is full.
         * Returns null if the fill rate is zero or negative (not filling up).
         */
        fun formatEstimate(
            availableBytes: Long,
            bytesPerDay: Long,
        ): String? = storageGrowthAnalyzer.formatEstimate(availableBytes, bytesPerDay)
    }
