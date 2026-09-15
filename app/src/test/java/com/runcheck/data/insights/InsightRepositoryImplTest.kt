package com.runcheck.data.insights

import com.google.gson.Gson
import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.db.entity.InsightEntity
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.util.TestAppDispatchers
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class InsightRepositoryImplTest {
    private val transactionRunner = DatabaseTransactionRunner { block -> block() }

    @Test
    fun `matching expired insight retains seen state during regeneration`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            val rows = mutableListOf(insightEntity(id = 5L, seen = true, expiresAt = 500L))
            coEvery { insightDao.getByRules(setOf("rule")) } answers { rows.toList() }
            coEvery { insightDao.deleteExpired(500L) } answers {
                rows.removeAll { !it.dismissed && it.expiresAt <= 500L }
            }

            repository.replaceGenerationResults(mapOf("rule" to listOf(insightCandidate())), now = 500L)

            val inserted = slot<List<InsightEntity>>()
            coVerify(exactly = 1) { insightDao.insertAll(capture(inserted)) }
            assertEquals(5L, inserted.captured.single().id)
            assertEquals(true, inserted.captured.single().seen)
        }

    @Test
    fun `replaceGenerationResults preserves seen and dismissed state for matching dedupe keys`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            coEvery { insightDao.getByRules(setOf("battery_rule")) } returns
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
    fun `dismissed insight survives an empty generation and stays dismissed when it returns`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            val dismissed =
                insightEntity(
                    id = 5L,
                    ruleId = "battery_rule",
                    dedupeKey = "same",
                    dismissed = true,
                    seen = true,
                )
            coEvery { insightDao.getByRules(setOf("battery_rule")) } returns listOf(dismissed)

            repository.replaceGenerationResults(
                candidatesByRule = mapOf("battery_rule" to emptyList()),
                now = 500L,
            )
            repository.replaceGenerationResults(
                candidatesByRule =
                    mapOf(
                        "battery_rule" to
                            listOf(
                                insightCandidate(
                                    ruleId = "battery_rule",
                                    dedupeKey = "same",
                                ),
                            ),
                    ),
                now = 600L,
            )

            val inserted = slot<List<InsightEntity>>()
            coVerify(exactly = 1) { insightDao.deleteUndismissedByRules(setOf("battery_rule")) }
            coVerify(exactly = 0) { insightDao.deleteByIds(listOf(5L)) }
            coVerify(exactly = 1) { insightDao.insertAll(capture(inserted)) }
            assertEquals(5L, inserted.captured.single().id)
            assertEquals(true, inserted.captured.single().dismissed)
        }

    @Test
    fun `multiple rule results use batched dao operations`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)
            coEvery { insightDao.getByRules(setOf("battery_rule", "thermal_rule")) } returns emptyList()

            repository.replaceGenerationResults(
                candidatesByRule =
                    mapOf(
                        "battery_rule" to listOf(insightCandidate(ruleId = "battery_rule")),
                        "thermal_rule" to listOf(insightCandidate(ruleId = "thermal_rule")),
                    ),
                now = 500L,
            )

            val inserted = slot<List<InsightEntity>>()
            coVerify(exactly = 1) {
                insightDao.getByRules(setOf("battery_rule", "thermal_rule"))
            }
            coVerify(exactly = 1) { insightDao.insertAll(capture(inserted)) }
            assertEquals(setOf("battery_rule", "thermal_rule"), inserted.captured.map { it.ruleId }.toSet())
        }

    @Test
    fun `markSeen updates only the supplied insight ids`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val repository = createRepository(insightDao)

            repository.markSeen(setOf(2L, 4L))

            coVerify(exactly = 1) { insightDao.markSeen(setOf(2L, 4L)) }
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
    fun `typed candidates persist canonical keys and unchanged arguments`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            coEvery { insightDao.getByRules(setOf("rule")) } returns emptyList()
            val candidates =
                InsightMessageId.entries.mapIndexed { index, id ->
                    insightCandidate(dedupeKey = "message_$index", bodyArgs = listOf("first", "second", "third"))
                        .copy(messageId = id)
                }

            createRepository(insightDao).replaceGenerationResults(mapOf("rule" to candidates), now = 500L)

            val inserted = slot<List<InsightEntity>>()
            coVerify(exactly = 1) { insightDao.insertAll(capture(inserted)) }
            assertEquals(candidates.size, inserted.captured.size)
            candidates.zip(inserted.captured).forEach { (candidate, entity) ->
                assertEquals(candidate.messageId.titleKey, entity.titleKey)
                assertEquals(candidate.messageId.bodyKey, entity.bodyKey)
                assertEquals(candidate.dedupeKey, entity.dedupeKey)
                assertEquals("""["first","second","third"]""", entity.bodyArgsJson)
            }
        }

    @Test
    fun `read side preserves legacy unknown partial and mismatched raw pairs`() =
        runTest {
            val insightDao: InsightDao = mockk(relaxed = true)
            val pairs =
                listOf(
                    "insight_app_battery_impact_title" to "insight_app_battery_impact_body",
                    "unknown_title" to "unknown_body",
                    "unknown_title" to "insight_battery_degradation_body",
                    "insight_battery_degradation_title" to "unknown_body",
                    "insight_battery_degradation_title" to "insight_storage_pressure_body",
                )
            val rows =
                pairs.mapIndexed { index, (title, body) ->
                    insightEntity(id = index.toLong(), expiresAt = Long.MAX_VALUE)
                        .copy(titleKey = title, bodyKey = body, bodyArgsJson = """["one","two","three"]""")
                }
            every { insightDao.observeUndismissedInsights() } returns flowOf(rows)

            val actual = createRepository(insightDao).getActiveInsights().first()

            assertEquals(pairs, actual.map { it.titleKey to it.bodyKey })
            actual.forEach { assertEquals(listOf("one", "two", "three"), it.bodyArgs) }
        }

    @Test
    fun `all persisted enum values decode exactly`() =
        runTest {
            val types =
                listOf(
                    "BATTERY" to InsightType.BATTERY,
                    "THERMAL" to InsightType.THERMAL,
                    "NETWORK" to InsightType.NETWORK,
                    "STORAGE" to InsightType.STORAGE,
                    "CHARGER" to InsightType.CHARGER,
                    "APP_USAGE" to InsightType.APP_USAGE,
                    "CROSS_CATEGORY" to InsightType.CROSS_CATEGORY,
                )
            val targets =
                listOf(
                    "NONE" to InsightTarget.NONE,
                    "BATTERY" to InsightTarget.BATTERY,
                    "THERMAL" to InsightTarget.THERMAL,
                    "NETWORK" to InsightTarget.NETWORK,
                    "STORAGE" to InsightTarget.STORAGE,
                    "CHARGER" to InsightTarget.CHARGER,
                    "APP_USAGE" to InsightTarget.APP_USAGE,
                )
            val priorities = listOf(0 to InsightPriority.HIGH, 1 to InsightPriority.MEDIUM, 2 to InsightPriority.LOW)

            assertEquals(
                types.map { it.second },
                readRows(types.map { activeEntity().copy(type = it.first) }).map { it.type },
            )
            assertEquals(
                targets.map { it.second },
                readRows(targets.map { activeEntity().copy(target = it.first) }).map { it.target },
            )
            assertEquals(
                priorities.map { it.second },
                readRows(priorities.map { activeEntity().copy(priority = it.first) }).map { it.priority },
            )
        }

    @Test
    fun `unknown empty wrong-case and padded types are omitted`() =
        runTest {
            for (type in listOf("FUTURE_TYPE", "", "battery", " BATTERY ")) {
                assertEquals(type, emptyList<Insight>(), readRows(listOf(activeEntity().copy(type = type))))
            }
        }

    @Test
    fun `unknown empty wrong-case and padded targets are omitted`() =
        runTest {
            for (target in listOf("FUTURE_TARGET", "", "battery", " BATTERY ")) {
                assertEquals(target, emptyList<Insight>(), readRows(listOf(activeEntity().copy(target = target))))
            }
        }

    @Test
    fun `unsupported priority numbers are omitted`() =
        runTest {
            for (priority in listOf(-1, 3, 99)) {
                assertEquals(emptyList<Insight>(), readRows(listOf(activeEntity().copy(priority = priority))))
            }
        }

    @Test
    fun `JSON parsing failures omit only the row`() =
        runTest {
            for (json in listOf("[", "{}", "\"text\"", "[\"ok\",{}]", "[\"ok\",[]]")) {
                assertEquals(json, emptyList<Insight>(), readRows(listOf(activeEntity().copy(bodyArgsJson = json))))
            }
        }

    @Test
    fun `accepted Gson argument representations remain unchanged`() =
        runTest {
            val cases =
                listOf(
                    "[\"25\",\"warm\"]" to listOf("25", "warm"),
                    "[]" to emptyList(),
                    "null" to emptyList(),
                    "" to emptyList(),
                    " \t\r\n " to emptyList(),
                    "[25,true,false]" to listOf("25", "true", "false"),
                    "[\"ok\",null]" to listOf("ok", null),
                )
            for ((json, expected) in cases) {
                val row =
                    activeEntity().copy(
                        bodyKey = "insight_app_battery_impact_body",
                        bodyArgsJson = json,
                        confidence = -1f,
                    )
                val result = readRows(listOf(row)).single()
                assertEquals(json, expected, result.bodyArgs)
                assertEquals(-1f, result.confidence, 0f)
            }
        }

    @Test
    fun `mixed rows retain DAO order without mutating stored rows`() =
        runTest {
            val dao: InsightDao = mockk()
            val rows =
                listOf(
                    activeEntity(9L),
                    activeEntity(2L).copy(type = "FUTURE"),
                    activeEntity(4L),
                    activeEntity(5L).copy(bodyArgsJson = "["),
                    activeEntity(1L),
                )
            every { dao.observeUndismissedInsights() } returns flowOf(rows)

            assertEquals(listOf(9L, 4L, 1L), createRepository(dao).getActiveInsights().first().map { it.id })
            verify(exactly = 1) { dao.observeUndismissedInsights() }
            confirmVerified(dao)
        }

    @Test
    fun `all-invalid emission is empty and subsequent valid emission still arrives`() =
        runTest {
            val dao: InsightDao = mockk()
            every { dao.observeUndismissedInsights() } returns
                flow {
                    emit(listOf(activeEntity().copy(target = "FUTURE"), activeEntity().copy(bodyArgsJson = "[")))
                    emit(listOf(activeEntity(7L)))
                }

            val emissions = createRepository(dao).getActiveInsights().toList()
            assertEquals(listOf(emptyList<Long>(), listOf(7L)), emissions.map { rows -> rows.map { it.id } })
        }

    @Test
    fun `DAO failure and cancellation propagate`() =
        runTest {
            for (failure in listOf(IllegalStateException("database failed"), CancellationException("cancelled"))) {
                val dao: InsightDao = mockk()
                every { dao.observeUndismissedInsights() } returns
                    flow {
                        throw failure
                    }
                try {
                    createRepository(dao).getActiveInsights().first()
                    fail("Expected DAO failure to propagate")
                } catch (actual: Exception) {
                    assertEquals(failure.javaClass, actual.javaClass)
                    assertEquals(failure.message, actual.message)
                }
            }
        }

    @Test
    fun `expired rows are filtered before JSON decoding`() =
        runTest {
            val dao: InsightDao = mockk()
            val gson: Gson = mockk()
            every { dao.observeUndismissedInsights() } returns
                flowOf(listOf(activeEntity().copy(expiresAt = 0L, bodyArgsJson = "[")))
            val repository = InsightRepositoryImpl(dao, gson, transactionRunner, TestAppDispatchers())

            assertEquals(emptyList<Insight>(), repository.getActiveInsights().first())
            verify { gson wasNot Called }
        }

    @Test
    fun `regeneration repairs undecodable rows preserving identity and user state`() =
        runTest {
            for (dismissed in listOf(false, true)) {
                val dao: InsightDao = mockk(relaxed = true)
                val original =
                    activeEntity(
                        5L,
                    ).copy(type = "FUTURE", bodyArgsJson = "[", seen = true, dismissed = dismissed)
                var stored = original
                every { dao.observeUndismissedInsights() } returns
                    flow {
                        emit(listOf(stored).filterNot { it.dismissed })
                    }
                coEvery { dao.getByRules(setOf("rule")) } answers { listOf(stored) }
                coEvery { dao.insertAll(any()) } answers { stored = firstArg<List<InsightEntity>>().single() }
                val repository = createRepository(dao)

                assertEquals(emptyList<Insight>(), repository.getActiveInsights().first())
                assertEquals(original, stored)
                repository.replaceGenerationResults(
                    mapOf("rule" to listOf(insightCandidate(bodyArgs = listOf("25")).copy(expiresAt = Long.MAX_VALUE))),
                    now = System.currentTimeMillis(),
                )

                assertEquals(5L, stored.id)
                assertEquals(original.ruleId, stored.ruleId)
                assertEquals(original.dedupeKey, stored.dedupeKey)
                assertEquals(true, stored.seen)
                assertEquals(dismissed, stored.dismissed)
                assertEquals("BATTERY", stored.type)
                assertEquals("[\"25\"]", stored.bodyArgsJson)
                val result = repository.getActiveInsights().first()
                if (dismissed) {
                    assertEquals(emptyList<Insight>(), result)
                } else {
                    assertEquals(5L, result.single().id)
                    assertEquals(listOf("25"), result.single().bodyArgs)
                    assertEquals(true, result.single().seen)
                }
            }
        }

    private fun activeEntity(id: Long = 1L): InsightEntity = insightEntity(id = id, expiresAt = Long.MAX_VALUE)

    private suspend fun readRows(rows: List<InsightEntity>): List<Insight> {
        val dao: InsightDao = mockk()
        every { dao.observeUndismissedInsights() } returns flowOf(rows)
        return createRepository(dao).getActiveInsights().first()
    }

    private fun createRepository(insightDao: InsightDao): InsightRepositoryImpl =
        InsightRepositoryImpl(
            insightDao = insightDao,
            gson = Gson(),
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
            messageId = InsightMessageId.BATTERY_DEGRADATION,
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
