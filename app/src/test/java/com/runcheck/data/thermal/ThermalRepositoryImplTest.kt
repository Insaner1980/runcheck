package com.runcheck.data.thermal

import android.os.SystemClock
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.data.device.DeviceProfile
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.usecase.GetThermalStateUseCase
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.testutil.assertRepositoryReads
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalRepositoryImplTest {
    @Before
    fun setUpClock() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 1000L
    }

    @After
    fun resetClock() {
        unmockkStatic(SystemClock::class)
    }

    @Test
    fun `live measurement survives event persistence failure`() =
        runTest {
            val events = mockk<ThrottlingRepository>(relaxed = true)
            coEvery { events.getOpenEvent() } returns null
            coEvery { events.insert(any()) } throws IllegalStateException("event database full")
            val liveRepository = liveRepository(events)

            val state = GetThermalStateUseCase(liveRepository)().first()

            assertEquals(42f, state.batteryTempC)
            assertEquals(ThermalStatus.SEVERE, state.thermalStatus)
        }

    @Test
    fun `live tracking recovers on next measurement and collectors share one event`() =
        runTest {
            val events = mockk<ThrottlingRepository>(relaxed = true)
            coEvery { events.getOpenEvent() } returns null
            var failInsert = true
            val stored = mutableListOf<ThrottlingEvent>()
            coEvery { events.insert(any()) } coAnswers {
                if (failInsert) error("event database full")
                stored.add(firstArg())
                7L
            }
            coEvery { events.updateSnapshot(any(), any(), any(), any(), any()) } coAnswers {
                stored[0] = stored[0].copy(thermalStatus = secondArg())
            }
            coEvery { events.updateDuration(any(), any()) } coAnswers {
                stored[0] = stored[0].copy(durationMs = secondArg())
            }
            val statuses = MutableStateFlow(ThermalStatus.SEVERE)
            val repository = liveRepository(events, statuses)
            val first = mutableListOf<ThermalState>()
            val second = mutableListOf<ThermalState>()
            val firstCollector = backgroundScope.launch { GetThermalStateUseCase(repository)().collect { first.add(it) } }
            val secondCollector = backgroundScope.launch { GetThermalStateUseCase(repository)().collect { second.add(it) } }
            runCurrent()
            assertEquals(ThermalStatus.SEVERE, first.last().thermalStatus)
            assertEquals(first, second)
            assertEquals(0, stored.size)

            failInsert = false
            statuses.value = ThermalStatus.CRITICAL
            runCurrent()
            statuses.value = ThermalStatus.EMERGENCY
            runCurrent()
            statuses.value = ThermalStatus.NONE
            runCurrent()

            assertEquals(
                listOf(ThermalStatus.SEVERE, ThermalStatus.CRITICAL, ThermalStatus.EMERGENCY, ThermalStatus.NONE),
                first.map { it.thermalStatus },
            )
            assertEquals(first, second)
            assertEquals(1, stored.size)
            assertEquals(ThermalStatus.EMERGENCY.name, stored.single().thermalStatus)
            assertEquals(0L, stored.single().durationMs)
            firstCollector.cancel()
            secondCollector.cancel()
            runCurrent()
            assertEquals(0, statuses.subscriptionCount.value)
        }

    @Test
    fun `monitoring collection still propagates event persistence failure`() =
        runTest {
            val events = mockk<ThrottlingRepository>(relaxed = true)
            val failure = IllegalStateException("event database full")
            coEvery { events.getOpenEvent() } throws failure

            val thrown = runCatching { liveRepository(events).getThermalState().first() }.exceptionOrNull()

            assertEquals(failure.message, thrown?.message)
        }

    @Test
    fun `live collection propagates event cancellation`() =
        runTest {
            val events = mockk<ThrottlingRepository>(relaxed = true)
            coEvery { events.getOpenEvent() } throws CancellationException("tracking cancelled")

            val thrown = runCatching { GetThermalStateUseCase(liveRepository(events))().first() }.exceptionOrNull()

            assertTrue(thrown is CancellationException)
        }

    private fun liveRepository(
        events: ThrottlingRepository,
        statuses: Flow<ThermalStatus> = flowOf(ThermalStatus.SEVERE),
    ): ThermalRepositoryImpl {
        val source = mockk<ThermalDataSource>()
        every { source.getBatteryTemperature() } returns flowOf(42f)
        every { source.getCpuTemperature(emptyList()) } returns flowOf(null)
        every { source.getThermalStatus() } returns statuses
        every { source.getThermalHeadroom() } returns flowOf(null)
        val profile = mockk<DeviceProfileProvider>()
        coEvery { profile.getDeviceProfile() } returns deviceProfile()
        return ThermalRepositoryImpl(
            thermalDataSource = source,
            deviceProfileProvider = profile,
            thermalReadingDao = thermalReadingDao,
            trackThrottlingEvents = TrackThrottlingEventsUseCase(events, mockk(relaxed = true)),
            dispatchers = TestAppDispatchers(),
        )
    }

    private val thermalReadingDao: ThermalReadingDao = mockk(relaxed = true)
    private val repository =
        ThermalRepositoryImpl(
            thermalDataSource = mockk(relaxed = true),
            deviceProfileProvider = mockk<DeviceProfileProvider>(relaxed = true),
            thermalReadingDao = thermalReadingDao,
            trackThrottlingEvents = mockk<TrackThrottlingEventsUseCase>(relaxed = true),
            dispatchers = TestAppDispatchers(),
        )

    @Test
    fun `reading queries map entities to domain models`() =
        runTest {
            val entity = thermalReadingEntity()
            val expected = thermalReading()
            every { thermalReadingDao.getReadingsSince(10L) } returns flowOf(listOf(entity))
            every { thermalReadingDao.getReadingsSinceLimited(10L, 1) } returns flowOf(listOf(entity))
            coEveryReadings()

            assertRepositoryReads(
                listOf(expected),
                { repository.getReadingsSince(10L, it) },
                { repository.getReadingsSinceSync(10L) },
                repository::getAllReadings,
            )
        }

    @Test
    fun `save and delete methods delegate mapped values to dao`() =
        runTest {
            val inserted = slot<ThermalReadingEntity>()

            repository.saveReading(
                ThermalState(
                    batteryTempC = 42.5f,
                    cpuTempC = 55.5f,
                    thermalStatus = ThermalStatus.SEVERE,
                    isThrottling = true,
                ),
            )
            repository.deleteOlderThan(1_000L)
            repository.deleteAll()

            coVerify(exactly = 1) { thermalReadingDao.insert(capture(inserted)) }
            assertEquals(42.5f, inserted.captured.batteryTempC)
            assertEquals(55.5f, inserted.captured.cpuTempC)
            assertEquals(ThermalStatus.SEVERE.ordinal, inserted.captured.thermalStatus)
            assertEquals(true, inserted.captured.throttling)
            coVerify(exactly = 1) { thermalReadingDao.deleteOlderThan(1_000L) }
            coVerify(exactly = 1) { thermalReadingDao.deleteAll() }
        }

    @Test
    fun `thermal state source failures propagate to collectors`() =
        runTest {
            val failure = IllegalStateException("thermal failed")
            val thermalDataSource: ThermalDataSource = mockk()
            val deviceProfileProvider: DeviceProfileProvider = mockk()
            every { thermalDataSource.getBatteryTemperature() } returns flow { throw failure }
            every { thermalDataSource.getCpuTemperature(emptyList()) } returns flowOf(null)
            every { thermalDataSource.getThermalStatus() } returns flowOf(ThermalStatus.NONE)
            every { thermalDataSource.getThermalHeadroom() } returns flowOf(null)
            coEvery { deviceProfileProvider.getDeviceProfile() } returns deviceProfile()
            val repository =
                ThermalRepositoryImpl(
                    thermalDataSource = thermalDataSource,
                    deviceProfileProvider = deviceProfileProvider,
                    thermalReadingDao = thermalReadingDao,
                    trackThrottlingEvents = mockk<TrackThrottlingEventsUseCase>(relaxed = true),
                    dispatchers = TestAppDispatchers(),
                )

            try {
                repository.getThermalState().first()
                fail("Expected thermal source failure to reach the collector")
            } catch (error: IllegalStateException) {
                assertEquals(failure.message, error.message)
            }
        }

    private fun coEveryReadings() {
        io.mockk.coEvery { thermalReadingDao.getReadingsSinceSync(10L) } returns listOf(thermalReadingEntity())
        io.mockk.coEvery { thermalReadingDao.getAll() } returns listOf(thermalReadingEntity())
    }

    private fun deviceProfile(): DeviceProfile =
        DeviceProfile(
            manufacturer = "google",
            model = "Pixel 8",
            apiLevel = 34,
            currentNowReliable = true,
            currentNowUnit = CurrentUnit.MICROAMPS,
            currentNowSignConvention = SignConvention.POSITIVE_CHARGING,
            cycleCountAvailable = true,
            thermalZonesAvailable = emptyList(),
            storageHealthAvailable = true,
        )

    private fun thermalReadingEntity(): ThermalReadingEntity =
        ThermalReadingEntity(
            id = 2L,
            timestamp = 1_234L,
            batteryTempC = 42.5f,
            cpuTempC = 55.5f,
            thermalStatus = ThermalStatus.SEVERE.ordinal,
            throttling = true,
        )

    private fun thermalReading(): ThermalReading =
        ThermalReading(
            timestamp = 1_234L,
            batteryTempC = 42.5f,
            cpuTempC = 55.5f,
            thermalStatus = ThermalStatus.SEVERE.ordinal,
            throttling = true,
        )
}
