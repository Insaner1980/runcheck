package com.runcheck.domain.insights.rules

import androidx.paging.PagingData
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.AppUsageListSummary
import com.runcheck.domain.repository.AppBatteryUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeavyAppUsageRuleTest {
    @Test
    fun `returns app usage insight when one app dominates the day`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 10L * hourMs
            val readings =
                listOf(
                    AppBatteryUsage(
                        timestamp = now - 3L * hourMs,
                        packageName = "video.app",
                        appLabel = "VideoApp",
                        foregroundTimeMs =
                            2L * hourMs,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsage(
                        timestamp = now - 2L * hourMs,
                        packageName = "video.app",
                        appLabel = "VideoApp",
                        foregroundTimeMs =
                            90L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsage(
                        timestamp = now - 2L * hourMs,
                        packageName = "chat.app",
                        appLabel = "ChatApp",
                        foregroundTimeMs =
                            45L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsage(
                        timestamp = now - 1L * hourMs,
                        packageName = "maps.app",
                        appLabel = "Maps",
                        foregroundTimeMs =
                            20L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                )
            val rule = HeavyAppUsageRule(FakeAppBatteryUsageRepository(readings))

            val insights = rule.evaluate(now)

            assertEquals(1, insights.size)
            val insight = insights.single()
            assertEquals(HeavyAppUsageRule.RULE_ID, insight.ruleId)
            assertEquals("video.app:70plus", insight.dedupeKey)
            assertEquals("VideoApp", insight.bodyArgs[0])
            assertEquals("76", insight.bodyArgs[1])
            assertEquals("3h 30m", insight.bodyArgs[2])
        }

    @Test
    fun `returns empty when usage is too distributed or too small`() =
        runTest {
            val hourMs = 60L * 60L * 1000L
            val now = 10L * hourMs
            val readings =
                listOf(
                    AppBatteryUsage(
                        timestamp = now - 3L * hourMs,
                        packageName = "video.app",
                        appLabel = "VideoApp",
                        foregroundTimeMs =
                            70L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsage(
                        timestamp = now - 2L * hourMs,
                        packageName = "chat.app",
                        appLabel = "ChatApp",
                        foregroundTimeMs =
                            65L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                    AppBatteryUsage(
                        timestamp = now - 1L * hourMs,
                        packageName = "maps.app",
                        appLabel = "Maps",
                        foregroundTimeMs =
                            40L * 60L * 1000L,
                        estimatedDrainMah = null,
                    ),
                )
            val rule = HeavyAppUsageRule(FakeAppBatteryUsageRepository(readings))

            val insights = rule.evaluate(now)

            assertTrue(insights.isEmpty())
        }
}

private class FakeAppBatteryUsageRepository(
    private val usages: List<AppBatteryUsage>,
) : AppBatteryUsageRepository {
    override fun getAggregatedUsageSince(since: Long): Flow<PagingData<AppBatteryUsage>> = emptyFlow()

    override fun getUsageSummarySince(since: Long): Flow<AppUsageListSummary> = emptyFlow()

    override suspend fun getUsageSinceSync(since: Long): List<AppBatteryUsage> = usages.filter { it.timestamp >= since }

    override suspend fun collectUsageSnapshot() = Unit

    override suspend fun deleteOlderThan(cutoff: Long) = Unit

    override suspend fun deleteAll() = Unit
}
