package com.runcheck.data.battery

import android.database.sqlite.SQLiteException
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.device.DeviceProfile
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryRepositoryImplTest {
    @Test
    fun `concurrent first consumers share one source and later consumers reuse it`() =
        runTest {
            val fixture = SourceFixture()
            val profileReady = CompletableDeferred<DeviceProfile>()
            coEvery { fixture.provider.getDeviceProfile() } coAnswers { profileReady.await() }

            val first = async(start = CoroutineStart.UNDISPATCHED) { fixture.repository.getBatteryState().first() }
            val second = async(start = CoroutineStart.UNDISPATCHED) { fixture.repository.getBatteryState().first() }
            assertFalse(first.isCompleted)
            assertFalse(second.isCompleted)
            coVerify(exactly = 1) { fixture.provider.getDeviceProfile() }
            verify(exactly = 0) { fixture.factory.create(any()) }

            profileReady.complete(fixture.profile)
            assertSame(fixture.current, first.await().currentMa)
            assertSame(fixture.current, second.await().currentMa)
            assertSame(
                fixture.current,
                fixture.repository
                    .getBatteryState()
                    .first()
                    .currentMa,
            )
            coVerify(exactly = 1) { fixture.provider.getDeviceProfile() }
            verify(exactly = 1) { fixture.factory.create(fixture.profile) }
            verify(exactly = 3) { fixture.source.getCurrentNow() }
        }

    @Test
    fun `failed factory construction propagates and retries before caching success`() =
        runTest {
            val fixture = SourceFixture()
            val failure = IllegalStateException("source construction failed")
            every { fixture.factory.create(fixture.profile) } throws failure

            assertSame(failure, runCatching { fixture.repository.getBatteryState().first() }.exceptionOrNull())
            coVerify(exactly = 1) { fixture.provider.getDeviceProfile() }
            verify(exactly = 1) { fixture.factory.create(fixture.profile) }
            verify(exactly = 0) { fixture.source.getCurrentNow() }

            every { fixture.factory.create(fixture.profile) } returns fixture.source
            repeat(2) {
                assertSame(
                    fixture.current,
                    fixture.repository
                        .getBatteryState()
                        .first()
                        .currentMa,
                )
            }
            coVerify(exactly = 2) { fixture.provider.getDeviceProfile() }
            verify(exactly = 2) { fixture.factory.create(fixture.profile) }
            verify(exactly = 2) { fixture.source.getCurrentNow() }
        }

    @Test
    fun `failed profile retrieval propagates without construction and retries before caching success`() =
        runTest {
            val fixture = SourceFixture()
            val failure = IllegalStateException("profile retrieval failed")
            coEvery { fixture.provider.getDeviceProfile() } throws failure

            assertSame(failure, runCatching { fixture.repository.getBatteryState().first() }.exceptionOrNull())
            coVerify(exactly = 1) { fixture.provider.getDeviceProfile() }
            verify(exactly = 0) { fixture.factory.create(any()) }

            coEvery { fixture.provider.getDeviceProfile() } returns fixture.profile
            repeat(2) {
                assertSame(
                    fixture.current,
                    fixture.repository
                        .getBatteryState()
                        .first()
                        .currentMa,
                )
            }
            coVerify(exactly = 2) { fixture.provider.getDeviceProfile() }
            verify(exactly = 1) { fixture.factory.create(fixture.profile) }
            verify(exactly = 2) { fixture.source.getCurrentNow() }
        }

    @Test
    fun `cancelling profile retrieval releases initialization mutex for a waiting consumer`() =
        runTest {
            val fixture = SourceFixture()
            val profileReady = CompletableDeferred<DeviceProfile>()
            coEvery { fixture.provider.getDeviceProfile() } coAnswers { profileReady.await() }
            val first = async(start = CoroutineStart.UNDISPATCHED) { fixture.repository.getBatteryState().first() }
            val waiting = async(start = CoroutineStart.UNDISPATCHED) { fixture.repository.getBatteryState().first() }
            coVerify(exactly = 1) { fixture.provider.getDeviceProfile() }

            first.cancelAndJoin()
            assertTrue(first.isCancelled)
            verify(exactly = 0) { fixture.factory.create(any()) }
            profileReady.complete(fixture.profile)

            assertSame(fixture.current, waiting.await().currentMa)
            assertSame(
                fixture.current,
                fixture.repository
                    .getBatteryState()
                    .first()
                    .currentMa,
            )
            coVerify(exactly = 2) { fixture.provider.getDeviceProfile() }
            verify(exactly = 1) { fixture.factory.create(fixture.profile) }
        }

    @Test
    fun `estimateFullCapacityMah estimates full battery capacity from charge counter and level`() {
        assertEquals(4_000, estimateFullCapacityMah(2_000, 50))
        assertEquals(4_500, estimateFullCapacityMah(3_375, 75))
    }

    @Test
    fun `estimateFullCapacityMah rejects unavailable or implausible inputs`() {
        assertEquals(null, estimateFullCapacityMah(null, 50))
        assertEquals(null, estimateFullCapacityMah(2_000, 0))
        assertEquals(null, estimateFullCapacityMah(2_000, 101))
        assertEquals(null, estimateFullCapacityMah(1, 100))
        assertEquals(null, estimateFullCapacityMah(50_000, 50))
    }

    @Test
    fun `estimateFullCapacityMah handles the one percent boundary without rounding`() {
        assertEquals(500, estimateFullCapacityMah(5, 1))
        assertEquals(20_000, estimateFullCapacityMah(200, 1))
        assertEquals(null, estimateFullCapacityMah(201, 1))
    }

    @Test
    fun `saveReading stores null current when confidence is unavailable`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            val repository = createRepository(dao)

            repository.saveReading(
                BatteryState(
                    level = 55,
                    voltageMv = 3900,
                    temperatureC = 31f,
                    currentMa = MeasuredValue(0, Confidence.UNAVAILABLE),
                    chargingStatus = ChargingStatus.DISCHARGING,
                    plugType = PlugType.NONE,
                    health = BatteryHealth.GOOD,
                    technology = "Li-ion",
                ),
            )

            val slot = slot<BatteryReadingEntity>()
            coVerify { dao.insert(capture(slot)) }
            assertEquals(null, slot.captured.currentMa)
            assertEquals(Confidence.UNAVAILABLE.name, slot.captured.currentConfidence)
        }

    @Test
    fun `saveReading propagates database failures`() =
        runTest {
            val failure = SQLiteException("database full")
            val dao: BatteryReadingDao = mockk(relaxed = true)
            coEvery { dao.insert(any()) } throws failure
            val repository = createRepository(dao)

            val thrown =
                runCatching {
                    repository.saveReading(
                        BatteryState(
                            level = 55,
                            voltageMv = 3900,
                            temperatureC = 31f,
                            currentMa = MeasuredValue(-250, Confidence.HIGH),
                            chargingStatus = ChargingStatus.DISCHARGING,
                            plugType = PlugType.NONE,
                            health = BatteryHealth.GOOD,
                            technology = "Li-ion",
                        ),
                    )
                }.exceptionOrNull()

            assertSame(failure, thrown)
        }

    @Test
    fun `getReadingsSince filters unusable timestamps after DAO returns rows`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            every { dao.getReadingsSince(10L) } returns
                flowOf(
                    listOf(
                        batteryReadingEntity(timestamp = -1L),
                        batteryReadingEntity(timestamp = 123L),
                    ),
                )
            val repository = createRepository(dao)

            val result = repository.getReadingsSince(since = 10L, limit = null).first()

            assertEquals(listOf(123L), result.map { it.timestamp })
        }

    @Test
    fun `getReadingsSinceSync filters unusable timestamps after DAO returns rows`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            coEvery { dao.getReadingsSinceSync(10L) } returns
                listOf(
                    batteryReadingEntity(timestamp = -1L),
                    batteryReadingEntity(timestamp = 123L),
                )
            val repository = createRepository(dao)

            val result = repository.getReadingsSinceSync(since = 10L)

            assertEquals(listOf(123L), result.map { it.timestamp })
        }

    @Test
    fun `getReadingsSince delegates to limited query when limit is provided`() =
        runTest {
            val dao: BatteryReadingDao = mockk(relaxed = true)
            every { dao.getReadingsSinceLimited(10L, 1) } returns
                flowOf(listOf(batteryReadingEntity(timestamp = 123L)))
            val repository = createRepository(dao)

            val result = repository.getReadingsSince(since = 10L, limit = 1).first()

            assertEquals(1, result.size)
            assertEquals(123L, result.single().timestamp)
        }

    private class SourceFixture {
        val profile = DeviceProfile(manufacturer = "samsung")
        val provider = mockk<DeviceProfileProvider>()
        val factory = mockk<BatteryDataSourceFactory>()
        val source = mockk<BatteryDataSource>()
        val current = MeasuredValue(-250, Confidence.HIGH)
        val repository = BatteryRepositoryImpl(factory, provider, mockk(), TestAppDispatchers())

        init {
            coEvery { provider.getDeviceProfile() } returns profile
            every { factory.create(profile) } returns source
            every { source.getLevel() } returns flowOf(55)
            every { source.getVoltage() } returns flowOf(3900)
            every { source.getTemperature() } returns flowOf(31f)
            every { source.getCurrentNow() } returns flowOf(current)
            every { source.getChargingStatus() } returns flowOf(ChargingStatus.DISCHARGING)
            every { source.getPlugType() } returns flowOf(PlugType.NONE)
            every { source.getHealth() } returns flowOf(BatteryHealth.GOOD)
            every { source.getTechnology() } returns flowOf("Li-ion")
            every { source.getCycleCount() } returns flowOf(null)
            every { source.getHealthPercent() } returns flowOf(null)
            every { source.getChargeCounter() } returns flowOf(null)
        }
    }

    private fun createRepository(dao: BatteryReadingDao): BatteryRepositoryImpl =
        BatteryRepositoryImpl(
            batteryDataSourceFactory = mockk(relaxed = true),
            deviceProfileProvider = mockk<DeviceProfileProvider>(relaxed = true),
            batteryReadingDao = dao,
            dispatchers = TestAppDispatchers(),
        )

    private fun batteryReadingEntity(timestamp: Long): BatteryReadingEntity =
        BatteryReadingEntity(
            id = timestamp,
            timestamp = timestamp,
            level = 55,
            voltageMv = 3900,
            temperatureC = 31f,
            currentMa = -250,
            currentConfidence = Confidence.HIGH.name,
            status = ChargingStatus.DISCHARGING.name,
            plugType = PlugType.NONE.name,
            health = BatteryHealth.GOOD.name,
            cycleCount = null,
            healthPct = null,
        )
}
