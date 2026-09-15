package com.runcheck.domain.usecase

import com.runcheck.domain.insights.engine.InsightEngine
import com.runcheck.domain.insights.engine.InsightRule
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.InsightRepository
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringDataResetTest {
    @Test
    fun `same severity after reset opens one fresh event and does not inherit duration`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val oldId =
                fixture.events.rows
                    .single()
                    .id

            fixture.clear()
            fixture.observe(200)
            fixture.observe(250)
            fixture.observe(300, ThermalStatus.NONE)

            val event = fixture.events.rows.single()
            assertNotEquals(oldId, event.id)
            assertEquals(200L, event.timestamp)
            assertEquals(100L, event.durationMs)
        }

    // CPD-OFF: Keep this concurrency scenario self-contained and readable.
    @Test
    fun `old history generation cannot survive reset and new history can generate`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val consumed = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            fixture.afterRead = {
                consumed.complete(Unit)
                release.await()
            }
            val generation = launch { fixture.engine.generateInsights(150) }
            consumed.await()
            var resetCompleted = false
            val reset =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    fixture.clear()
                    resetCompleted = true
                }
            fixture.insights.onPublish = { assertTrue("Old history published after reset", !resetCompleted) }
            release.complete(Unit)
            generation.join()
            reset.join()
            assertTrue(fixture.insights.rows.isEmpty())

            fixture.insights.onPublish = {}
            fixture.afterRead = {}
            fixture.observe(200)
            fixture.engine.generateInsights(250)
            assertEquals(listOf("200"), fixture.insights.rows.map { it.dedupeKey })
        }
    // CPD-ON

    @Test
    fun `cancelling a waiting reset leaves admitted generation and event intact`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val oldId =
                fixture.events.rows
                    .single()
                    .id
            val consumed = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            fixture.afterRead = {
                consumed.complete(Unit)
                release.await()
            }
            val generation = launch { fixture.engine.generateInsights(150) }
            consumed.await()
            val reset = launch(start = CoroutineStart.UNDISPATCHED) { fixture.clear() }
            reset.cancelAndJoin()
            release.complete(Unit)
            generation.join()
            fixture.observe(200)
            assertEquals(
                oldId,
                fixture.events.rows
                    .single()
                    .id,
            )
            fixture.clear()
            assertTrue(fixture.events.rows.isEmpty())
        }

    @Test
    fun `cancelled generation releases reset and later generation`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val consumed = CompletableDeferred<Unit>()
            fixture.afterRead = {
                consumed.complete(Unit)
                awaitCancellation()
            }
            val generation = launch { fixture.engine.generateInsights(150) }
            consumed.await()
            val reset = launch(start = CoroutineStart.UNDISPATCHED) { fixture.clear() }
            generation.cancelAndJoin()
            reset.join()
            assertTrue(fixture.insights.rows.isEmpty())
            fixture.afterRead = {}
            fixture.observe(200)
            fixture.engine.generateInsights(250)
            assertEquals(
                "200",
                fixture.insights.rows
                    .single()
                    .dedupeKey,
            )
        }

    @Test
    fun `cleanup failure after deletion does not retain deleted event`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            coEvery { fixture.preferences.clearMonitoringDataState() } throws IllegalStateException("cleanup")
            assertTrue(runCatching { fixture.clear() }.exceptionOrNull() is IllegalStateException)
            fixture.observe(200)
            assertEquals(
                200L,
                fixture.events.rows
                    .single()
                    .timestamp,
            )
            coEvery { fixture.preferences.clearMonitoringDataState() } returns Unit
            fixture.clear()
            assertTrue(fixture.events.rows.isEmpty())
        }

    @Test
    fun `cancellation during cleanup leaves tracker ready for post reset observations`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val cleanupStarted = CompletableDeferred<Unit>()
            coEvery { fixture.preferences.clearMonitoringDataState() } coAnswers {
                cleanupStarted.complete(Unit)
                awaitCancellation()
            }
            val reset = launch { fixture.clear() }
            cleanupStarted.await()
            reset.cancelAndJoin()
            fixture.observe(200)
            assertEquals(
                200L,
                fixture.events.rows
                    .single()
                    .timestamp,
            )
            coEvery { fixture.preferences.clearMonitoringDataState() } returns Unit
            fixture.clear()
        }

    @Test
    fun `reset waits for an in flight thermal insert and then clears its identity`() =
        runTest {
            val fixture = Fixture()
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            fixture.beforeInsert = {
                entered.complete(Unit)
                release.await()
            }
            val observation = launch { fixture.observe(100) }
            entered.await()
            val reset = launch(start = CoroutineStart.UNDISPATCHED) { fixture.clear() }
            release.complete(Unit)
            observation.join()
            reset.join()
            assertTrue(fixture.events.rows.isEmpty())
            fixture.beforeInsert = {}
            fixture.observe(200)
            assertEquals(
                200L,
                fixture.events.rows
                    .single()
                    .timestamp,
            )
        }

    @Test
    fun `failed rule evaluation releases coordination for reset and next generation`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            fixture.afterRead = { error("rule failed") }
            assertTrue(runCatching { fixture.engine.generateInsights(150) }.exceptionOrNull() is IllegalStateException)
            assertTrue(fixture.insights.rows.isEmpty())
            fixture.clear()
            fixture.afterRead = {}
            fixture.observe(200)
            fixture.engine.generateInsights(250)
            assertEquals(
                "200",
                fixture.insights.rows
                    .single()
                    .dedupeKey,
            )
        }

    @Test
    fun `cancellation at transaction return cannot leave committed deletion cached`() =
        runTest {
            val fixture = Fixture()
            fixture.observe(100)
            val committed = CompletableDeferred<Unit>()
            fixture.afterTransaction = {
                committed.complete(Unit)
                awaitCancellation()
            }
            val reset = launch { fixture.clear() }
            committed.await()
            reset.cancelAndJoin()
            fixture.observe(200)
            assertEquals(
                200L,
                fixture.events.rows
                    .single()
                    .timestamp,
            )
            fixture.afterTransaction = {}
            fixture.clear()
        }

    private class Fixture {
        val events = EventStore()
        val insights = InsightStore()
        val preferences = mockk<UserPreferencesRepository>(relaxed = true)
        var beforeInsert: suspend () -> Unit = {}
        var afterTransaction: suspend () -> Unit = {}
        val tracker =
            TrackThrottlingEventsUseCase(
                events,
                object : TrackThrottlingEventsUseCase.ForegroundAppProvider {
                    override suspend fun getCurrentForegroundApp(): String? {
                        beforeInsert()
                        return null
                    }
                },
            )
        private val coordinator = MonitoringDataCoordinator(tracker)
        var afterRead: suspend () -> Unit = {}
        private val rule =
            object : InsightRule {
                override val ruleId = "history"

                override suspend fun evaluate(now: Long): List<InsightCandidate> {
                    val history = events.getEventsSinceSync(0)
                    afterRead()
                    return history.map { event ->
                        InsightCandidate(
                            ruleId = ruleId,
                            dedupeKey = event.timestamp.toString(),
                            type = InsightType.THERMAL,
                            priority = InsightPriority.HIGH,
                            confidence = 0.9f,
                            messageId = InsightMessageId.RECURRING_THERMAL_THROTTLING,
                            bodyArgs = emptyList(),
                            generatedAt = now,
                            expiresAt = now + 1000,
                            dataWindowStart = event.timestamp,
                            dataWindowEnd = now,
                            target = InsightTarget.THERMAL,
                        )
                    }
                }
            }
        val engine = InsightEngine(setOf(rule), insights, coordinator)

        // This fixture proves coordination, not Room rollback.
        val clear =
            ClearMonitoringDataUseCase(
                transactionRunner =
                    DatabaseTransactionRunner {
                        it()
                        afterTransaction()
                    },
                batteryRepository = mockk(relaxed = true),
                networkRepository = mockk(relaxed = true),
                thermalRepository = mockk(relaxed = true),
                storageRepository = mockk(relaxed = true),
                throttlingRepository = events,
                appBatteryUsageRepository = mockk(relaxed = true),
                speedTestRepository = mockk(relaxed = true),
                insightRepository = insights,
                chargerRepository = mockk(relaxed = true),
                userPreferencesRepository = preferences,
                monitoringAlertStateRepository = mockk(relaxed = true),
                monitoringStatusRepository = mockk(relaxed = true),
                fileExportRepository = mockk(relaxed = true),
                monitoringDataCoordinator = coordinator,
            )

        suspend fun observe(
            time: Long,
            status: ThermalStatus = ThermalStatus.SEVERE,
        ) {
            tracker(
                ThermalState(40f, thermalStatus = status, isThrottling = status >= ThermalStatus.SEVERE),
                time,
                time,
            )
        }
    }

    private class EventStore : ThrottlingRepository by mockk<ThrottlingRepository>() {
        val rows = mutableListOf<ThrottlingEvent>()
        private var nextId = 1L

        override suspend fun insert(event: ThrottlingEvent): Long {
            val id = nextId++
            rows.add(event.copy(id = id))
            return id
        }

        override suspend fun getOpenEvent(): ThrottlingEvent? = rows.firstOrNull { it.durationMs == null }

        override suspend fun getEventsSinceSync(since: Long): List<ThrottlingEvent> =
            rows.filter {
                it.timestamp >=
                    since
            }

        override suspend fun deleteAll() = rows.clear()

        override suspend fun updateDuration(
            id: Long,
            durationMs: Long,
        ) {
            val index = rows.indexOfFirst { it.id == id }
            if (index >= 0) rows[index] = rows[index].copy(durationMs = durationMs)
        }
    }

    private class InsightStore : InsightRepository by mockk<InsightRepository>() {
        var rows = emptyList<InsightCandidate>()
        var onPublish: () -> Unit = {}

        override suspend fun clearAll() {
            rows = emptyList()
        }

        override suspend fun replaceGenerationResults(
            candidatesByRule: Map<String, List<InsightCandidate>>,
            now: Long,
        ) {
            onPublish()
            rows = candidatesByRule.values.flatten()
        }
    }
}
