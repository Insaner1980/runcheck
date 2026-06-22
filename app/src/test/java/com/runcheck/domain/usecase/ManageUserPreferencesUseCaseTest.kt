package com.runcheck.domain.usecase

import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ManageUserPreferencesUseCaseTest {
    private val repository: UserPreferencesRepository = mockk(relaxed = true)
    private val useCase = ManageUserPreferencesUseCase(repository)

    @Test
    fun `observe methods delegate to repository flows`() =
        runTest {
            val preferences = UserPreferences(alertBatteryThreshold = 15)
            every { repository.getPreferences() } returns flowOf(preferences)
            every { repository.observeSelectedChargerId() } returns flowOf(7L)

            assertEquals(preferences, useCase.observePreferences().first())
            assertEquals(7L, useCase.observeSelectedChargerId().first())
        }

    @Test
    fun `setter methods delegate to repository`() =
        runTest {
            useCase.setSelectedChargerId(4L)
            useCase.setNotifLowBattery(false)
            useCase.setNotifHighTemp(false)
            useCase.setNotifLowStorage(false)
            useCase.setNotifChargeComplete(true)
            useCase.setAlertBatteryThreshold(10)
            useCase.setAlertTempThreshold(39)
            useCase.setAlertStorageThreshold(85)
            useCase.setTemperatureUnit(TemperatureUnit.FAHRENHEIT)
            useCase.setLiveNotificationEnabled(true)
            useCase.setLiveNotifCurrent(false)
            useCase.setLiveNotifDrainRate(false)
            useCase.setLiveNotifTemperature(false)
            useCase.setLiveNotifScreenStats(true)
            useCase.setLiveNotifRemainingTime(true)
            useCase.setShowInfoCards(false)

            coVerify(exactly = 1) { repository.setSelectedChargerId(4L) }
            coVerify(exactly = 1) { repository.setNotifLowBattery(false) }
            coVerify(exactly = 1) { repository.setNotifHighTemp(false) }
            coVerify(exactly = 1) { repository.setNotifLowStorage(false) }
            coVerify(exactly = 1) { repository.setNotifChargeComplete(true) }
            coVerify(exactly = 1) { repository.setAlertBatteryThreshold(10) }
            coVerify(exactly = 1) { repository.setAlertTempThreshold(39) }
            coVerify(exactly = 1) { repository.setAlertStorageThreshold(85) }
            coVerify(exactly = 1) { repository.setTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
            coVerify(exactly = 1) { repository.setLiveNotificationEnabled(true) }
            coVerify(exactly = 1) { repository.setLiveNotifCurrent(false) }
            coVerify(exactly = 1) { repository.setLiveNotifDrainRate(false) }
            coVerify(exactly = 1) { repository.setLiveNotifTemperature(false) }
            coVerify(exactly = 1) { repository.setLiveNotifScreenStats(true) }
            coVerify(exactly = 1) { repository.setLiveNotifRemainingTime(true) }
            coVerify(exactly = 1) { repository.setShowInfoCards(false) }
        }

    @Test
    fun `reset alert thresholds restores default preference values`() =
        runTest {
            useCase.resetAlertThresholds()

            coVerify(exactly = 1) { repository.setAlertBatteryThreshold(20) }
            coVerify(exactly = 1) { repository.setAlertTempThreshold(42) }
            coVerify(exactly = 1) { repository.setAlertStorageThreshold(90) }
        }
}
