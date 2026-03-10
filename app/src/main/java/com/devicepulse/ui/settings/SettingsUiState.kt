package com.devicepulse.ui.settings

import com.devicepulse.data.device.DeviceProfile
import com.devicepulse.domain.model.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val deviceProfile: DeviceProfile? = null,
    val isPro: Boolean = false,
    val proPrice: String? = null,
    val exportStatus: String? = null
)
