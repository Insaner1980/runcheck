package com.devicepulse.domain.usecase

import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CleanupOldReadingsUseCaseTest {

    private lateinit var batteryDao: BatteryReadingDao
    private lateinit var networkDao: NetworkReadingDao
    private lateinit var thermalDao: ThermalReadingDao
    private lateinit var storageDao: StorageReadingDao
    private lateinit var proStatusRepository: ProStatusRepository
    private lateinit var useCase: CleanupOldReadingsUseCase

    @Before
    fun setup() {
        batteryDao = mockk(relaxed = true)
        networkDao = mockk(relaxed = true)
        thermalDao = mockk(relaxed = true)
        storageDao = mockk(relaxed = true)
        proStatusRepository = mockk()

        useCase = CleanupOldReadingsUseCase(
            batteryDao, networkDao, thermalDao, storageDao, proStatusRepository
        )
    }

    @Test
    fun `free user triggers cleanup of all tables`() = runTest {
        every { proStatusRepository.isPro() } returns false

        useCase()

        coVerify { batteryDao.deleteOlderThan(any()) }
        coVerify { networkDao.deleteOlderThan(any()) }
        coVerify { thermalDao.deleteOlderThan(any()) }
        coVerify { storageDao.deleteOlderThan(any()) }
    }

    @Test
    fun `pro user does not trigger cleanup`() = runTest {
        every { proStatusRepository.isPro() } returns true

        useCase()

        coVerify(exactly = 0) { batteryDao.deleteOlderThan(any()) }
        coVerify(exactly = 0) { networkDao.deleteOlderThan(any()) }
        coVerify(exactly = 0) { thermalDao.deleteOlderThan(any()) }
        coVerify(exactly = 0) { storageDao.deleteOlderThan(any()) }
    }
}
