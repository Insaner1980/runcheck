package com.devicepulse.ui.settings

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.model.UserPreferences

@Immutable
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val deviceProfile: DeviceProfileInfo? = null,
    val isPro: Boolean = false,
    val billingAvailable: Boolean = false,
    val proPrice: String? = null,
    val billingStatus: String? = null,
    val isExporting: Boolean = false,
    val exportUris: List<Uri>? = null,
    val exportStatus: String? = null,
    val errorMessage: String? = null
)
