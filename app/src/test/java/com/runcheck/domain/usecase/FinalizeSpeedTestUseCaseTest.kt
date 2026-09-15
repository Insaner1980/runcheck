package com.runcheck.domain.usecase

import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.SpeedTestRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
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
    fun `free user saves and trims to five results`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase(result)

            coVerify(exactly = 1) { speedTestRepository.saveResultAndTrim(result, 5) }
            coVerify(exactly = 0) { speedTestRepository.saveResult(any()) }
        }

    @Test
    fun `pro user saves and trims to one hundred results`() =
        runTest {
            every { proStatusProvider.isPro() } returns true

            useCase(result)

            coVerify(exactly = 1) { speedTestRepository.saveResultAndTrim(result, 100) }
            coVerify(exactly = 0) { speedTestRepository.saveResult(any()) }
        }

    @Test
    fun `finalization checks current entitlement for each result`() =
        runTest {
            every { proStatusProvider.isPro() } returnsMany listOf(false, true, false)

            repeat(3) { useCase(result) }

            coVerify(exactly = 2) { speedTestRepository.saveResultAndTrim(result, 5) }
            coVerify(exactly = 1) { speedTestRepository.saveResultAndTrim(result, 100) }
        }

    @Test
    fun `persistence failure propagates unchanged`() =
        runTest {
            every { proStatusProvider.isPro() } returns false
            val failure = IllegalStateException("Persistence failed")
            coEvery { speedTestRepository.saveResultAndTrim(any(), any()) } throws failure

            assertSame(failure, runCatching { useCase(result) }.exceptionOrNull())
        }

    @Test
    fun `cancellation propagates unchanged`() =
        runTest {
            every { proStatusProvider.isPro() } returns true
            val cancellation = CancellationException("Cancelled")
            coEvery { speedTestRepository.saveResultAndTrim(any(), any()) } throws cancellation

            assertSame(cancellation, runCatching { useCase(result) }.exceptionOrNull())
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
