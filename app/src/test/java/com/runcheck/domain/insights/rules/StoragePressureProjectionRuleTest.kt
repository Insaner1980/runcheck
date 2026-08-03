package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.StorageGrowthAnalyzer
import com.runcheck.domain.insights.model.InsightPriority
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
            val readings = projectionReadings(40_000L, 30_000L, 20_000L, 10_000L)
            val rule =
                StoragePressureProjectionRule(
                    storageRepository = TestStorageRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                )

            val insights = rule.evaluate(NOW)

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
            val readings = projectionReadings(40_000L, 45_000L, 50_000L, 55_000L)
            val rule =
                StoragePressureProjectionRule(
                    storageRepository = TestStorageRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                )

            val insights = rule.evaluate(NOW)

            assertTrue(insights.isEmpty())
        }

    @Test
    fun `returns medium priority projection for thirty day bucket`() =
        runTest {
            val readings = projectionReadings(88_000L, 82_000L, 76_000L, 70_000L)
            val rule =
                StoragePressureProjectionRule(
                    storageRepository = TestStorageRepository(readings),
                    storageGrowthAnalyzer = StorageGrowthAnalyzer(),
                )

            val insight = rule.evaluate(NOW).single()

            assertEquals("30d", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
        }

    private fun storage(
        timestamp: Long,
        availableBytes: Long,
    ) = StorageReading(
        timestamp = timestamp,
        totalBytes = 100_000L,
        availableBytes = availableBytes,
        appsBytes = 0L,
        mediaBytes = 0L,
    )

    private fun projectionReadings(vararg availableBytes: Long) =
        availableBytes.mapIndexed { index, bytes ->
            storage(NOW - (6L - index * 2L) * DAY_MS, bytes)
        }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val NOW = 30L * DAY_MS
    }
}
