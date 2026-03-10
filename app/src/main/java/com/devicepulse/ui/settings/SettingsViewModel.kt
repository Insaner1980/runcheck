package com.devicepulse.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.device.DeviceProfileRepository
import com.devicepulse.data.preferences.UserPreferencesRepository
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val proStatusRepository: ProStatusRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.getPreferences(),
                deviceProfileRepository.getProfile(),
                proStatusRepository.isProUser
            ) { prefs, profile, isPro ->
                SettingsUiState(
                    preferences = prefs,
                    deviceProfile = profile,
                    isPro = isPro
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        viewModelScope.launch {
            val details = proStatusRepository.queryProductDetails()
            details?.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                _uiState.value = _uiState.value.copy(proPrice = price)
            }
        }
    }

    fun purchasePro(activity: Activity) {
        proStatusRepository.launchPurchaseFlow(activity)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAmoledBlack(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAmoledBlack(enabled) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDynamicColors(enabled) }
    }

    fun setMonitoringInterval(interval: MonitoringInterval) {
        viewModelScope.launch { preferencesRepository.setMonitoringInterval(interval) }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }
}
