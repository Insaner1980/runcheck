package com.runcheck.domain.usecase

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CleanupOldReadingsUseCaseTest {
    private lateinit var transactionRunner: DatabaseTransactionRunner
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var chargerRepository: ChargerRepository
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
        chargerRepository = mockk(relaxed = true)
        throttlingRepository = mockk(relaxed = true)
        appBatteryUsageRepository = mockk(relaxed = true)
        speedTestRepository = mockk(relaxed = true)
        proStatusProvider = mockk()
        userPreferencesRepository = mockk()

        coEvery { transactionRunner.runInTransaction(any()) } coAnswers {
            firstArg<suspend () -> Unit>().invoke()
        }
        every { userPreferencesRepository.getPreferences() } returns flowOf(UserPreferences())

        useCase =
            CleanupOldReadingsUseCase(
                transactionRunner,
                batteryRepository,
                networkRepository,
                thermalRepository,
                storageRepository,
                chargerRepository,
                throttlingRepository,
                appBatteryUsageRepository,
                speedTestRepository,
                proStatusProvider,
                userPreferencesRepository,
            )
    }

    @Suppress("LongMethod")
    @Test
    fun `free user triggers cleanup with 24-hour cutoff`() =
        runTest {
            every { proStatusProvider.isPro() } returns false
            val beforeInvoke = System.currentTimeMillis()

            useCase()

            val afterInvoke = System.currentTimeMillis()
            val twentyFourHoursMs = 24 * 60 * 60 * 1000L
            val toleranceMs = 5_000L

            // Verify all repositories receive a cutoff approximately 24 hours ago
            coVerify {
                batteryRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                networkRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                thermalRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                storageRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                chargerRepository.deleteSessionsOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                throttlingRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                appBatteryUsageRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                speedTestRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - twentyFourHoursMs,
                            afterInvoke - twentyFourHoursMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
        }

    @Test
    fun `pro user with forever retention does not trigger cleanup`() =
        runTest {
            every { proStatusProvider.isPro() } returns true
            every { userPreferencesRepository.getPreferences() } returns
                flowOf(
                    UserPreferences(dataRetention = DataRetention.FOREVER),
                )

            useCase()

            coVerify(exactly = 0) { transactionRunner.runInTransaction(any()) }
            coVerify(exactly = 0) { batteryRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { networkRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { thermalRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { storageRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { chargerRepository.deleteSessionsOlderThan(any()) }
            coVerify(exactly = 0) { throttlingRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { appBatteryUsageRepository.deleteOlderThan(any()) }
            coVerify(exactly = 0) { speedTestRepository.deleteOlderThan(any()) }
        }

    @Test
    fun `pro user with six months retention triggers cleanup with correct cutoff`() =
        runTest {
            every { proStatusProvider.isPro() } returns true
            every { userPreferencesRepository.getPreferences() } returns
                flowOf(
                    UserPreferences(dataRetention = DataRetention.SIX_MONTHS),
                )
            val beforeInvoke = System.currentTimeMillis()

            useCase()

            val afterInvoke = System.currentTimeMillis()
            val sixMonthsMs = 180L * 24 * 60 * 60 * 1000
            val toleranceMs = 5_000L

            coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
            coVerify {
                batteryRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - sixMonthsMs, afterInvoke - sixMonthsMs, toleranceMs)
                    },
                )
            }
            coVerify {
                networkRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - sixMonthsMs, afterInvoke - sixMonthsMs, toleranceMs)
                    },
                )
            }
            coVerify {
                chargerRepository.deleteSessionsOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - sixMonthsMs, afterInvoke - sixMonthsMs, toleranceMs)
                    },
                )
            }
        }

    @Suppress("LongMethod")
    @Test
    fun `pro user with three months retention triggers cleanup with correct cutoff`() =
        runTest {
            every { proStatusProvider.isPro() } returns true
            every { userPreferencesRepository.getPreferences() } returns
                flowOf(
                    UserPreferences(dataRetention = DataRetention.THREE_MONTHS),
                )
            val beforeInvoke = System.currentTimeMillis()

            useCase()

            val afterInvoke = System.currentTimeMillis()
            val threeMonthsMs = 90L * 24 * 60 * 60 * 1000
            val toleranceMs = 5_000L

            coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
            coVerify {
                batteryRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - threeMonthsMs,
                            afterInvoke - threeMonthsMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                thermalRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - threeMonthsMs,
                            afterInvoke - threeMonthsMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                storageRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - threeMonthsMs,
                            afterInvoke - threeMonthsMs,
                            toleranceMs,
                        )
                    },
                )
            }
            coVerify {
                chargerRepository.deleteSessionsOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(
                            cutoff,
                            beforeInvoke - threeMonthsMs,
                            afterInvoke - threeMonthsMs,
                            toleranceMs,
                        )
                    },
                )
            }
        }

    @Test
    fun `pro user with one year retention triggers cleanup with correct cutoff`() =
        runTest {
            every { proStatusProvider.isPro() } returns true
            every { userPreferencesRepository.getPreferences() } returns
                flowOf(
                    UserPreferences(dataRetention = DataRetention.ONE_YEAR),
                )
            val beforeInvoke = System.currentTimeMillis()

            useCase()

            val afterInvoke = System.currentTimeMillis()
            val oneYearMs = 365L * 24 * 60 * 60 * 1000
            val toleranceMs = 5_000L

            coVerify(exactly = 1) { transactionRunner.runInTransaction(any()) }
            coVerify {
                batteryRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - oneYearMs, afterInvoke - oneYearMs, toleranceMs)
                    },
                )
            }
            coVerify {
                networkRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - oneYearMs, afterInvoke - oneYearMs, toleranceMs)
                    },
                )
            }
            coVerify {
                storageRepository.deleteOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - oneYearMs, afterInvoke - oneYearMs, toleranceMs)
                    },
                )
            }
            coVerify {
                chargerRepository.deleteSessionsOlderThan(
                    withArg { cutoff ->
                        assertCutoffInRange(cutoff, beforeInvoke - oneYearMs, afterInvoke - oneYearMs, toleranceMs)
                    },
                )
            }
        }

    /**
     * Assert that the cutoff timestamp falls within the expected range, accounting
     * for wall-clock time elapsed between capturing beforeInvoke and afterInvoke,
     * plus a tolerance for execution overhead.
     */
    private fun assertCutoffInRange(
        actual: Long,
        expectedMin: Long,
        expectedMax: Long,
        toleranceMs: Long,
    ) {
        assertTrue(
            "Cutoff $actual should be >= ${expectedMin - toleranceMs} " +
                "(expected min $expectedMin - tolerance $toleranceMs)",
            actual >= expectedMin - toleranceMs,
        )
        assertTrue(
            "Cutoff $actual should be <= ${expectedMax + toleranceMs} " +
                "(expected max $expectedMax + tolerance $toleranceMs)",
            actual <= expectedMax + toleranceMs,
        )
    }
}
