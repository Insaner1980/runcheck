package com.runcheck.domain.usecase

import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.DeviceProfileInfo
import com.runcheck.domain.model.SignConvention
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.DeviceProfileRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSettingsUseCaseTest {
    @Test
    fun `combines preferences with device profile`() =
        runTest {
            val preferences = UserPreferences(notificationsEnabled = false)
            val profile =
                DeviceProfileInfo(
                    manufacturer = "Example",
                    model = "Device",
                    apiLevel = 37,
                    currentNowReliable = true,
                    currentNowUnit = CurrentUnit.MICROAMPS,
                    currentNowSignConvention = SignConvention.POSITIVE_CHARGING,
                    cycleCountAvailable = true,
                    thermalZonesAvailable = emptyList(),
                    storageHealthAvailable = false,
                )
            val preferencesRepository = mockk<UserPreferencesRepository>()
            val profileRepository = mockk<DeviceProfileRepository>()
            every { preferencesRepository.getPreferences() } returns flowOf(preferences)
            every { profileRepository.getProfile() } returns flowOf(profile)

            val result = ObserveSettingsUseCase(preferencesRepository, profileRepository)().first()

            assertEquals(SettingsData(preferences, profile), result)
        }
}
