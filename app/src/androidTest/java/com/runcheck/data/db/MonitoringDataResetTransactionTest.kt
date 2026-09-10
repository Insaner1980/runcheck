package com.runcheck.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.data.thermal.ThrottlingRepositoryImpl
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.usecase.MonitoringDataCoordinator
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.util.AppDispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonitoringDataResetTransactionTest {
    private lateinit var database: RuncheckDatabase
    private lateinit var repository: ThrottlingRepositoryImpl
    private lateinit var tracker: TrackThrottlingEventsUseCase
    private lateinit var coordinator: MonitoringDataCoordinator
    private lateinit var transactions: RoomTransactionRunner

    @Before
    fun setUp() {
        database =
            Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                RuncheckDatabase::class.java,
            ).build()
        repository = ThrottlingRepositoryImpl(database.throttlingEventDao(), AppDispatchers())
        tracker =
            TrackThrottlingEventsUseCase(
                repository,
                object : TrackThrottlingEventsUseCase.ForegroundAppProvider {
                    override suspend fun getCurrentForegroundApp(): String? = null
                },
            )
        coordinator = MonitoringDataCoordinator(tracker)
        transactions = RoomTransactionRunner(database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun committedResetCreatesFreshEvent() =
        runBlocking {
            observe(100)
            val oldId = requireNotNull(repository.getOpenEvent()).id
            reset()
            observe(200)
            observe(250)
            observe(300, ThermalStatus.NONE)
            val event = repository.getEventsSinceSync(0).single()
            assertNotEquals(oldId, event.id)
            assertEquals(200L, event.timestamp)
            assertEquals(100L, event.durationMs)
        }

    @Test
    fun failedTransactionRestoresEventFromRoomAndAllowsLaterReset() =
        runBlocking {
            observe(100)
            val original = requireNotNull(repository.getOpenEvent())
            val failure = runCatching { reset { error("rollback") } }.exceptionOrNull()
            assertTrue(failure is IllegalStateException)
            assertEquals(original, repository.getEventsSinceSync(0).single())
            observe(200, ThermalStatus.NONE)
            assertEquals(original.copy(durationMs = 100L), repository.getEventsSinceSync(0).single())
            assertNull(repository.getOpenEvent())
            reset()
            assertTrue(repository.getEventsSinceSync(0).isEmpty())
            observe(300)
            assertNotEquals(original.id, requireNotNull(repository.getOpenEvent()).id)
        }

    @Test
    fun cancelledTransactionRollsBackAndDoesNotStrandTracker() =
        runBlocking {
            observe(100)
            val original = requireNotNull(repository.getOpenEvent())
            val deleted = CompletableDeferred<Unit>()
            val reset = launch {
                reset {
                    deleted.complete(Unit)
                    awaitCancellation()
                }
            }
            deleted.await()
            reset.cancelAndJoin()
            assertEquals(original, repository.getEventsSinceSync(0).single())
            observe(200, ThermalStatus.NONE)
            assertEquals(original.copy(durationMs = 100L), repository.getEventsSinceSync(0).single())
            assertNull(repository.getOpenEvent())
            reset()
            assertTrue(repository.getEventsSinceSync(0).isEmpty())
            observe(300)
            assertNotEquals(original.id, requireNotNull(repository.getOpenEvent()).id)
        }

    private suspend fun reset(afterDelete: suspend () -> Unit = {}) {
        coordinator.resetHistory {
            transactions.runInTransaction {
                repository.deleteAll()
                afterDelete()
            }
        }
    }

    private suspend fun observe(
        time: Long,
        status: ThermalStatus = ThermalStatus.SEVERE,
    ) {
        tracker(ThermalState(40f, thermalStatus = status, isThrottling = status >= ThermalStatus.SEVERE), time, time)
    }
}
