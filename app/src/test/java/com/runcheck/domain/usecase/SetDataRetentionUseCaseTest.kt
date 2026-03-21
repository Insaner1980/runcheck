package com.runcheck.domain.usecase

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SetDataRetentionUseCaseTest {

    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var cleanupOldReadingsUseCase: CleanupOldReadingsUseCase
    private lateinit var useCase: SetDataRetentionUseCase

    @Before
    fun setUp() {
        userPreferencesRepository = mockk(relaxed = true)
        proStatusProvider = mockk()
        cleanupOldReadingsUseCase = mockk(relaxed = true)

        useCase = SetDataRetentionUseCase(
            userPreferencesRepository = userPreferencesRepository,
            proStatusProvider = proStatusProvider,
            cleanupOldReadingsUseCase = cleanupOldReadingsUseCase
        )
    }

    @Test
    fun `non-pro user cannot update retention or trigger cleanup`() = runTest {
        every { proStatusProvider.isPro() } returns false

        useCase(DataRetention.ONE_YEAR)

        coVerify(exactly = 0) { userPreferencesRepository.setDataRetention(any()) }
        coVerify(exactly = 0) { cleanupOldReadingsUseCase.invoke() }
    }

    @Test
    fun `pro user updates retention and cleans up immediately`() = runTest {
        every { proStatusProvider.isPro() } returns true

        useCase(DataRetention.SIX_MONTHS)

        coVerify(exactly = 1) { userPreferencesRepository.setDataRetention(DataRetention.SIX_MONTHS) }
        coVerify(exactly = 1) { cleanupOldReadingsUseCase.invoke() }
    }
}
