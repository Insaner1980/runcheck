package com.runcheck.data.battery

import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.util.TestAppDispatchers
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BatteryRepositoryImplTest {
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

    private fun createRepository(dao: BatteryReadingDao): BatteryRepositoryImpl =
        BatteryRepositoryImpl(
            batteryDataSourceFactory = mockk(relaxed = true),
            deviceProfileProvider = mockk<DeviceProfileProvider>(relaxed = true),
            batteryReadingDao = dao,
            batteryCapacityReader = mockk(relaxed = true),
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
