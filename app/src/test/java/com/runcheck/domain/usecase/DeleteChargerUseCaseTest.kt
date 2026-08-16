package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DeleteChargerUseCaseTest {
    private val chargerRepository = mockk<ChargerRepository>()
    private val preferencesRepository = mockk<UserPreferencesRepository>()
    private val useCase = DeleteChargerUseCase(chargerRepository, preferencesRepository)

    @Test
    fun `clears selected charger before deleting it`() =
        runTest {
            coEvery { preferencesRepository.getSelectedChargerId() } returns 42L
            coJustRun { preferencesRepository.setSelectedChargerId(null) }
            coJustRun { chargerRepository.deleteChargerById(42L) }

            useCase(42L)

            coVerifyOrder {
                preferencesRepository.setSelectedChargerId(null)
                chargerRepository.deleteChargerById(42L)
            }
        }

    @Test
    fun `keeps another selected charger when deleting`() =
        runTest {
            coEvery { preferencesRepository.getSelectedChargerId() } returns 7L
            coJustRun { chargerRepository.deleteChargerById(42L) }

            useCase(42L)

            coVerify(exactly = 0) { preferencesRepository.setSelectedChargerId(any()) }
            coVerify(exactly = 1) { chargerRepository.deleteChargerById(42L) }
        }
}
