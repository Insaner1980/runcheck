package com.runcheck.domain.usecase

import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.SpeedTestRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class FinalizeSpeedTestUseCaseTest {
    private lateinit var speedTestRepository: SpeedTestRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var useCase: FinalizeSpeedTestUseCase

    @Before
    fun setUp() {
        speedTestRepository = mockk(relaxed = true)
        proStatusProvider = mockk()
        useCase = FinalizeSpeedTestUseCase(speedTestRepository, proStatusProvider)
    }

    @Test
    fun `free user saves and trims to the configured limit`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase(result, freeHistoryLimit = 5)

            coVerify(exactly = 1) { speedTestRepository.saveResultAndTrim(result, 5) }
            coVerify(exactly = 0) { speedTestRepository.saveResult(any()) }
        }

    @Test
    fun `pro user saves without trimming`() =
        runTest {
            every { proStatusProvider.isPro() } returns true

            useCase(result, freeHistoryLimit = 5)

            coVerify(exactly = 1) { speedTestRepository.saveResult(result) }
            coVerify(exactly = 0) { speedTestRepository.saveResultAndTrim(any(), any()) }
        }

    private val result =
        SpeedTestResult(
            timestamp = 1_000L,
            downloadMbps = 100.0,
            uploadMbps = 50.0,
            pingMs = 10,
            jitterMs = null,
            serverName = null,
            serverLocation = null,
            connectionType = ConnectionType.WIFI,
            networkSubtype = null,
            signalDbm = null,
        )
}
