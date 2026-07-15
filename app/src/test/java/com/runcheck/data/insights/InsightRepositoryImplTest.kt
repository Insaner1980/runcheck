package com.runcheck.data.insights

import com.google.gson.Gson
import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.db.entity.InsightEntity
import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InsightRepositoryImplTest {
    private val transactionRunner = DatabaseTransactionRunner { block -> block() }

    @Test
    fun `replaceGenerationResults preserves seen and dismissed state for matching dedupe keys`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            coEvery { insightDao.getByRule("battery_rule") } returns
                listOf(
                    insightEntity(
                        id = 5L,
                        ruleId = "battery_rule",
                        dedupeKey = "same",
                        dismissed = true,
                        seen = true,
                    ),
                    insightEntity(
                        id = 6L,
                        ruleId = "battery_rule",
                        dedupeKey = "stale",
                    ),
                )

            repository.replaceGenerationResults(
                candidatesByRule =
                    mapOf(
                        "battery_rule" to
                            listOf(
                                insightCandidate(
                                    ruleId = "battery_rule",
                                    dedupeKey = "same",
                                    bodyArgs = listOf("42", "fast"),
                                ),
                            ),
                    ),
                now = 500L,
            )

            val inserted = slot<List<InsightEntity>>()
            coVerify(exactly = 1) { insightDao.deleteByIds(listOf(6L)) }
            coVerify(exactly = 1) { insightDao.insertAll(capture(inserted)) }
            val merged = inserted.captured.single()
            assertEquals(5L, merged.id)
            assertEquals(true, merged.dismissed)
            assertEquals(true, merged.seen)
            assertEquals("""["42","fast"]""", merged.bodyArgsJson)
        }

    @Test
    fun `getActiveInsights filters expired rows and decodes body args`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val now = System.currentTimeMillis()
            every { insightDao.observeUndismissedInsights() } returns
                flowOf(
                    listOf(
                        insightEntity(
                            id = 1L,
                            bodyArgsJson = """["25","warm"]""",
                            expiresAt = now + 60_000L,
                        ),
                        insightEntity(
                            id = 2L,
                            expiresAt = now - 1L,
                        ),
                    ),
                )
            val repository = createRepository(insightDao)

            val result = repository.getActiveInsights().first()

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(1L, insight.id)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals(listOf("25", "warm"), insight.bodyArgs)
        }

    @Test
    fun `getUnseenCount counts only active unseen rows`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val now = System.currentTimeMillis()
            every { insightDao.observeUndismissedInsights() } returns
                flowOf(
                    listOf(
                        insightEntity(id = 1L, seen = false, expiresAt = now + 60_000L),
                        insightEntity(id = 2L, seen = true, expiresAt = now + 60_000L),
                        insightEntity(id = 3L, seen = false, expiresAt = now - 1L),
                    ),
                )
            val repository = createRepository(insightDao)

            assertEquals(1, repository.getUnseenCount().first())
        }

    private fun createRepository(insightDao: InsightDao): InsightRepositoryImpl =
        InsightRepositoryImpl(
            insightDao = insightDao,
            gson = Gson(),
            homeRankingPolicy = InsightHomeRankingPolicy(),
            transactionRunner = transactionRunner,
            dispatchers = TestAppDispatchers(),
        )

    private fun insightCandidate(
        ruleId: String = "rule",
        dedupeKey: String = "same",
        bodyArgs: List<String> = emptyList(),
    ): InsightCandidate =
        InsightCandidate(
            ruleId = ruleId,
            dedupeKey = dedupeKey,
            type = InsightType.BATTERY,
            priority = InsightPriority.HIGH,
            confidence = 0.8f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgs = bodyArgs,
            generatedAt = 1_000L,
            expiresAt = 2_000L,
            dataWindowStart = 100L,
            dataWindowEnd = 900L,
            target = InsightTarget.BATTERY,
        )

    private fun insightEntity(
        id: Long = 1L,
        ruleId: String = "rule",
        dedupeKey: String = "same",
        priority: Int = InsightPriority.HIGH.sortOrder,
        bodyArgsJson: String = "[]",
        generatedAt: Long = 1_000L,
        expiresAt: Long = generatedAt + 60_000L,
        dismissed: Boolean = false,
        seen: Boolean = false,
    ): InsightEntity =
        InsightEntity(
            id = id,
            ruleId = ruleId,
            dedupeKey = dedupeKey,
            type = InsightType.BATTERY.name,
            priority = priority,
            confidence = 0.8f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgsJson = bodyArgsJson,
            generatedAt = generatedAt,
            expiresAt = expiresAt,
            dataWindowStart = 0L,
            dataWindowEnd = 1L,
            target = InsightTarget.BATTERY.name,
            dismissed = dismissed,
            seen = seen,
        )
}
