package com.runcheck.domain.insights.rules

import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBatteryImpactRuleTest {
    @Test
    fun `returns app battery impact insight when one app dominates drain`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val readings =
                listOf(
                    usage(now - 20L * hourMs, "com.demo.streambox", "StreamBox", 2L * hourMs, 220f),
                    usage(now - 4L * hourMs, "com.demo.streambox", "StreamBox", 2L * hourMs, 280f),
                    usage(now - 11L * hourMs, "com.demo.mailbox", "Mailbox", 45L * 60L * 1000L, 36f),
                    usage(now - 7L * hourMs, "com.demo.maps", "City Maps", 30L * 60L * 1000L, 32f),
                )

            val rule = AppBatteryImpactRule(FakeImpactAppBatteryUsageRepository(readings))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(AppBatteryImpactRule.RULE_ID, insight.ruleId)
            assertEquals("com.demo.streambox:70plus", insight.dedupeKey)
            assertEquals("StreamBox", insight.bodyArgs[0])
            assertEquals("500", insight.bodyArgs[1])
            assertEquals("88", insight.bodyArgs[2])
        }

    @Test
    fun `returns empty when drain coverage is too small or spread out`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 100L * hourMs
            val readings =
                listOf(
                    usage(now - 20L * hourMs, "com.demo.streambox", "StreamBox", 2L * hourMs, 80f),
                    usage(now - 11L * hourMs, "com.demo.mailbox", "Mailbox", 45L * 60L * 1000L, 70f),
                    usage(now - 7L * hourMs, "com.demo.maps", "City Maps", 30L * 60L * 1000L, 60f),
                )

            val rule = AppBatteryImpactRule(FakeImpactAppBatteryUsageRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }

    private fun usage(
        timestamp: Long,
        packageName: String,
        appLabel: String,
        foregroundTimeMs: Long,
        estimatedDrainMah: Float?,
    ) = AppBatteryUsage(
        timestamp = timestamp,
        packageName = packageName,
        appLabel = appLabel,
        foregroundTimeMs = foregroundTimeMs,
        estimatedDrainMah = estimatedDrainMah,
    )
}

private class FakeImpactAppBatteryUsageRepository(
    private val readings: List<AppBatteryUsage>,
) : AppBatteryUsageRepository {
    override fun getAggregatedUsageSince(since: Long) = emptyFlow<androidx.paging.PagingData<AppBatteryUsage>>()

    override fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary> = emptyFlow()

    override suspend fun getUsageSinceSync(since: Long): List<AppBatteryUsage> =
        readings.filter { it.timestamp >= since }

    override suspend fun collectUsageSnapshot() = Unit

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
