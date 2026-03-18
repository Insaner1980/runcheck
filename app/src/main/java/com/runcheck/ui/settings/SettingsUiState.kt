package com.runcheck.ui.settings

import androidx.compose.runtime.Immutable
import com.runcheck.domain.model.DeviceProfileInfo
import com.runcheck.domain.model.UserPreferences
import com.runcheck.ui.common.UiText

@Immutable
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val deviceProfile: DeviceProfileInfo? = null,
    val isPro: Boolean = false,
    val billingAvailable: Boolean = false,
    val proPrice: String? = null,
    val billingStatus: UiText? = null,
    val isExporting: Boolean = false,
    val exportUris: List<String>? = null,
    val exportStatus: UiText? = null,
    val errorMessage: UiText? = null
)
