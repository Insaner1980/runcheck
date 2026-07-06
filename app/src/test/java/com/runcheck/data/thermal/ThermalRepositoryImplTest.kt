package com.runcheck.data.thermal

import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.data.device.DeviceProfile
import com.runcheck.data.device.DeviceProfileProvider
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.SignConvention
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.usecase.TrackThrottlingEventsUseCase
import com.runcheck.util.TestAppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ThermalRepositoryImplTest {
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

            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = null).first())
            assertEquals(listOf(expected), repository.getReadingsSince(10L, limit = 1).first())
            assertEquals(listOf(expected), repository.getReadingsSinceSync(10L))
            assertEquals(listOf(expected), repository.getAllReadings())
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
