package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageFillProjection
import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.repository.StorageRepository

internal const val STORAGE_PRESSURE_LOOKBACK_MS = 14L * 24L * 60L * 60L * 1000L

internal data class StoragePressureRuleData(
    val readings: List<StorageReading>,
    val projection: StorageFillProjection,
)

abstract class StoragePressureInsightRule(
    ruleId: String,
    repository: StorageRepository,
    analyzer: StorageGrowthAnalyzer,
    private val maxDaysUntilFull: Long,
) : SingleCandidateInsightRule(ruleId) {
    private val dataLoader = StoragePressureRuleDataLoader(repository, analyzer)

    final override suspend fun buildCandidate(now: Long): InsightCandidate? {
        val (readings, projection) = dataLoader.load(now, maxDaysUntilFull) ?: return null
        return buildStorageCandidate(now, readings.size, projection)
    }

    protected abstract fun buildStorageCandidate(
        now: Long,
        readingCount: Int,
        projection: StorageFillProjection,
    ): InsightCandidate?
}

internal class StoragePressureRuleDataLoader(
    private val repository: StorageRepository,
    private val analyzer: StorageGrowthAnalyzer,
) {
    suspend fun load(
        now: Long,
        maxDaysUntilFull: Long,
    ): StoragePressureRuleData? {
        val readings = repository.getReadingsSinceSync(now - STORAGE_PRESSURE_LOOKBACK_MS)
        if (readings.size < MINIMUM_STORAGE_PRESSURE_READING_COUNT) return null

        val projection = analyzer.calculateProjection(readings) ?: return null
        return StoragePressureRuleData(readings, projection)
            .takeIf { projection.daysUntilFull <= maxDaysUntilFull }
    }
}

private const val MINIMUM_STORAGE_PRESSURE_READING_COUNT = 4
