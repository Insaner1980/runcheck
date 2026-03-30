package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePressureImpactRuleTest {
    @Test
    fun `returns impact insight when storage score is degraded by pressure`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 20L * dayMs
            val totalBytes = 128L * GIB
            val readings =
                listOf(
                    storage(now - 10L * dayMs, totalBytes, 18L * GIB),
                    storage(now - 8L * dayMs, totalBytes, 16L * GIB),
                    storage(now - 6L * dayMs, totalBytes, 14L * GIB),
                    storage(now - 4L * dayMs, totalBytes, 12L * GIB),
                    storage(now - 2L * dayMs, totalBytes, 10L * GIB),
                    storage(now, totalBytes, 8L * GIB),
                )

            val rule =
                StoragePressureImpactRule(
                    storageRepository = FakeStorageImpactRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                    healthScoreCalculator = HealthScoreCalculator(),
                )

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(StoragePressureImpactRule.RULE_ID, insight.ruleId)
            assertEquals("storage_score:poor", insight.dedupeKey)
            assertEquals("45", insight.bodyArgs[0])
            assertEquals("1w", insight.bodyArgs[1])
        }

    @Test
    fun `returns empty when storage score stays healthy`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 20L * dayMs
            val totalBytes = 128L * GIB
            val readings =
                listOf(
                    storage(now - 10L * dayMs, totalBytes, 70L * GIB),
                    storage(now - 8L * dayMs, totalBytes, 69L * GIB),
                    storage(now - 6L * dayMs, totalBytes, 68L * GIB),
                    storage(now - 4L * dayMs, totalBytes, 67L * GIB),
                    storage(now - 2L * dayMs, totalBytes, 66L * GIB),
                    storage(now, totalBytes, 65L * GIB),
                )

            val rule =
                StoragePressureImpactRule(
                    storageRepository = FakeStorageImpactRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                    healthScoreCalculator = HealthScoreCalculator(),
                )

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    private fun storage(
        timestamp: Long,
        totalBytes: Long,
        availableBytes: Long,
    ) = StorageReading(
        timestamp = timestamp,
        totalBytes = totalBytes,
        availableBytes = availableBytes,
        appsBytes = ((totalBytes - availableBytes) * 0.62).toLong(),
        mediaBytes = ((totalBytes - availableBytes) * 0.28).toLong(),
    )

    private companion object {
        const val GIB = 1024L * 1024L * 1024L
    }
}

private class FakeStorageImpactRepository(
    private val readings: List<StorageReading>,
) : StorageRepository {
    override fun getStorageState(): Flow<StorageState> = emptyFlow()

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<StorageReading>> = emptyFlow()

    override suspend fun getReadingsSinceSync(since: Long): List<StorageReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun saveReading(state: StorageState) = Unit

    override suspend fun getAllReadings(): List<StorageReading> = readings

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
