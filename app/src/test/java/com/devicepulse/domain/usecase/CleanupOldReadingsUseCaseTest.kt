package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.NetworkRepository
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import com.devicepulse.domain.repository.SpeedTestRepository
import com.devicepulse.domain.repository.StorageRepository
import com.devicepulse.domain.repository.ThermalRepository
import com.devicepulse.domain.repository.ThrottlingRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CleanupOldReadingsUseCaseTest {

    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var throttlingRepository: ThrottlingRepository
    private lateinit var appBatteryUsageRepository: AppBatteryUsageRepository
    private lateinit var speedTestRepository: SpeedTestRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var useCase: CleanupOldReadingsUseCase

    @Before
    fun setup() {
        batteryRepository = mockk(relaxed = true)
        networkRepository = mockk(relaxed = true)
        thermalRepository = mockk(relaxed = true)
        storageRepository = mockk(relaxed = true)
        throttlingRepository = mockk(relaxed = true)
        appBatteryUsageRepository = mockk(relaxed = true)
        speedTestRepository = mockk(relaxed = true)
        proStatusProvider = mockk()

        useCase = CleanupOldReadingsUseCase(
            batteryRepository,
            networkRepository,
            thermalRepository,
            storageRepository,
            throttlingRepository,
            appBatteryUsageRepository,
            speedTestRepository,
            proStatusProvider
        )
    }

    @Test
    fun `free user triggers cleanup of all tables`() = runTest {
        every { proStatusProvider.isPro() } returns false

        useCase()

        coVerify { batteryRepository.deleteOlderThan(any()) }
        coVerify { networkRepository.deleteOlderThan(any()) }
        coVerify { thermalRepository.deleteOlderThan(any()) }
        coVerify { storageRepository.deleteOlderThan(any()) }
        coVerify { throttlingRepository.deleteOlderThan(any()) }
        coVerify { appBatteryUsageRepository.deleteOlderThan(any()) }
        coVerify { speedTestRepository.deleteOlderThan(any()) }
    }

    @Test
    fun `pro user does not trigger cleanup`() = runTest {
        every { proStatusProvider.isPro() } returns true

        useCase()

        coVerify(exactly = 0) { batteryRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { networkRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { thermalRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { storageRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { throttlingRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { appBatteryUsageRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { speedTestRepository.deleteOlderThan(any()) }
    }
}
