package com.runcheck.domain.usecase

import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class ClearMonitoringDataUseCaseTest {
    private lateinit var transactionRunner: DatabaseTransactionRunner
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var throttlingRepository: ThrottlingRepository
    private lateinit var appBatteryUsageRepository: AppBatteryUsageRepository
    private lateinit var speedTestRepository: SpeedTestRepository

    private lateinit var useCase: ClearMonitoringDataUseCase

    @Before
    fun setUp() {
        transactionRunner = DatabaseTransactionRunner { block -> block() }
        batteryRepository = mockk(relaxed = true)
        networkRepository = mockk(relaxed = true)
        thermalRepository = mockk(relaxed = true)
        storageRepository = mockk(relaxed = true)
        throttlingRepository = mockk(relaxed = true)
        appBatteryUsageRepository = mockk(relaxed = true)
        speedTestRepository = mockk(relaxed = true)

        useCase =
            ClearMonitoringDataUseCase(
                transactionRunner = transactionRunner,
                batteryRepository = batteryRepository,
                networkRepository = networkRepository,
                thermalRepository = thermalRepository,
                storageRepository = storageRepository,
                throttlingRepository = throttlingRepository,
                appBatteryUsageRepository = appBatteryUsageRepository,
                speedTestRepository = speedTestRepository,
            )
    }

    @Test
    fun `clears all monitoring repositories in one transaction`() =
        runTest {
            coEvery { batteryRepository.deleteAll() } returns Unit
            coEvery { networkRepository.deleteAll() } returns Unit
            coEvery { thermalRepository.deleteAll() } returns Unit
            coEvery { storageRepository.deleteAll() } returns Unit
            coEvery { throttlingRepository.deleteAll() } returns Unit
            coEvery { appBatteryUsageRepository.deleteAll() } returns Unit
            coEvery { speedTestRepository.deleteAll() } returns Unit

            useCase()

            coVerify(exactly = 1) { batteryRepository.deleteAll() }
            coVerify(exactly = 1) { networkRepository.deleteAll() }
            coVerify(exactly = 1) { thermalRepository.deleteAll() }
            coVerify(exactly = 1) { storageRepository.deleteAll() }
            coVerify(exactly = 1) { throttlingRepository.deleteAll() }
            coVerify(exactly = 1) { appBatteryUsageRepository.deleteAll() }
            coVerify(exactly = 1) { speedTestRepository.deleteAll() }
        }
}
