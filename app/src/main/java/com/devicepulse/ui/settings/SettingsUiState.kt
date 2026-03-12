package com.devicepulse.ui.settings

import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.model.UserPreferences

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val deviceProfile: DeviceProfileInfo? = null,
    val isPro: Boolean = false,
    val proPrice: String? = null,
    val billingStatus: String? = null,
    val exportStatus: String? = null,
    val errorMessage: String? = null
)
