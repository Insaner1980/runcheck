package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.DataRetention
import com.devicepulse.domain.model.UserPreferences
import com.devicepulse.domain.repository.DatabaseTransactionRunner
import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.NetworkRepository
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.repository.AppBatteryUsageRepository
import com.devicepulse.domain.repository.SpeedTestRepository
import com.devicepulse.domain.repository.StorageRepository
import com.devicepulse.domain.repository.ThermalRepository
import com.devicepulse.domain.repository.ThrottlingRepository
import com.devicepulse.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CleanupOldReadingsUseCaseTest {

    private lateinit var transactionRunner: DatabaseTransactionRunner
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var throttlingRepository: ThrottlingRepository
    private lateinit var appBatteryUsageRepository: AppBatteryUsageRepository
    private lateinit var speedTestRepository: SpeedTestRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var useCase: CleanupOldReadingsUseCase

    @Before
    fun setup() {
        transactionRunner = mockk()
        batteryRepository = mockk(relaxed = true)
        networkRepository = mockk(relaxed = true)
        thermalRepository = mockk(relaxed = true)
        storageRepository = mockk(relaxed = true)
        throttlingRepository = mockk(relaxed = true)
        appBatteryUsageRepository = mockk(relaxed = true)
        speedTestRepository = mockk(relaxed = true)
        proStatusProvider = mockk()
        userPreferencesRepository = mockk()

        coEvery { transactionRunner.runInTransaction(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        every { userPreferencesRepository.getPreferences() } returns flowOf(UserPreferences())

        useCase = CleanupOldReadingsUseCase(
            transactionRunner,
            batteryRepository,
            networkRepository,
            thermalRepository,
            storageRepository,
            throttlingRepository,
            appBatteryUsageRepository,
            speedTestRepository,
            proStatusProvider,
            userPreferencesRepository
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
        coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
    }

    @Test
    fun `pro user with forever retention does not trigger cleanup`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { userPreferencesRepository.getPreferences() } returns flowOf(
            UserPreferences(dataRetention = DataRetention.FOREVER)
        )

        useCase()

        coVerify(exactly = 0) { transactionRunner.runInTransaction(any()) }
        coVerify(exactly = 0) { batteryRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { networkRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { thermalRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { storageRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { throttlingRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { appBatteryUsageRepository.deleteOlderThan(any()) }
        coVerify(exactly = 0) { speedTestRepository.deleteOlderThan(any()) }
    }

    @Test
    fun `pro user with bounded retention triggers cleanup`() = runTest {
        every { proStatusProvider.isPro() } returns true
        every { userPreferencesRepository.getPreferences() } returns flowOf(
            UserPreferences(dataRetention = DataRetention.SIX_MONTHS)
        )

        useCase()

        coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
        coVerify { batteryRepository.deleteOlderThan(any()) }
        coVerify { networkRepository.deleteOlderThan(any()) }
        coVerify { thermalRepository.deleteOlderThan(any()) }
        coVerify { storageRepository.deleteOlderThan(any()) }
        coVerify { throttlingRepository.deleteOlderThan(any()) }
        coVerify { appBatteryUsageRepository.deleteOlderThan(any()) }
        coVerify { speedTestRepository.deleteOlderThan(any()) }
    }
}
