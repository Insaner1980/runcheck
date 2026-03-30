package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoragePressureProjectionRuleTest {
    @Test
    fun `returns storage insight when projection is under 30 days`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 30L * dayMs
            val readings =
                listOf(
                    StorageReading(
                        timestamp = now - 6L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 40_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now - 4L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 30_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now - 2L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 20_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now,
                        totalBytes = 100_000L,
                        availableBytes = 10_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                )
            val rule =
                StoragePressureProjectionRule(
                    storageRepository = FakeStorageRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                )

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(StoragePressureProjectionRule.RULE_ID, insight.ruleId)
            assertEquals("7d", insight.dedupeKey)
            assertEquals("insight_storage_pressure_title", insight.titleKey)
            assertEquals("insight_storage_pressure_body", insight.bodyKey)
            assertTrue(insight.bodyArgs.single().endsWith("d"))
        }

    @Test
    fun `returns empty when storage trend is stable or improving`() =
        runTest {
            val dayMs = 24L * 60L * 60L * 1000L
            val now = 30L * dayMs
            val readings =
                listOf(
                    StorageReading(
                        timestamp = now - 6L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 40_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now - 4L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 45_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now - 2L * dayMs,
                        totalBytes = 100_000L,
                        availableBytes = 50_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                    StorageReading(
                        timestamp = now,
                        totalBytes = 100_000L,
                        availableBytes = 55_000L,
                        appsBytes = 0L,
                        mediaBytes = 0L,
                    ),
                )
            val rule =
                StoragePressureProjectionRule(
                    storageRepository = FakeStorageRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                )

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}

private class FakeStorageRepository(
    private val readings: List<StorageReading>,
) : StorageRepository {
    override fun getStorageState(): Flow<StorageState> = emptyFlow()

    override suspend fun saveReading(state: StorageState) = Unit

    override fun getReadingsSince(
        since: Long,
        limit: Int?,
    ): Flow<List<StorageReading>> = emptyFlow()

    override suspend fun getReadingsSinceSync(since: Long): List<StorageReading> =
        readings.filter { it.timestamp >= since }

    override suspend fun getAllReadings(): List<StorageReading> = readings

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
