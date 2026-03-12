package com.devicepulse.ui.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.R
import com.devicepulse.billing.ProPurchaseManager
import com.devicepulse.domain.repository.DeviceProfileRepository
import com.devicepulse.domain.repository.FileExportRepository
import com.devicepulse.domain.repository.UserPreferencesRepository
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.model.ThemeMode
import com.devicepulse.domain.usecase.ExportDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val deviceProfileRepository: DeviceProfileRepository,
    private val proPurchaseManager: ProPurchaseManager,
    private val exportDataUseCase: ExportDataUseCase,
    private val fileExportRepository: FileExportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.getPreferences(),
                deviceProfileRepository.getProfile(),
                proPurchaseManager.isProUser
            ) { prefs, profile, isPro ->
                Triple(prefs, profile, isPro)
            }
                .catch {
                    _uiState.update { current ->
                        current.copy(errorMessage = context.getString(R.string.error_generic))
                    }
                }
                .collect { (prefs, profile, isPro) ->
                    _uiState.update { current ->
                        current.copy(
                            preferences = prefs,
                            deviceProfile = profile,
                            isPro = isPro
                        )
                    }
                }
        }
        viewModelScope.launch {
            try {
                proPurchaseManager.getFormattedPrice()?.let { price ->
                    _uiState.update { current -> current.copy(proPrice = price) }
                }
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(errorMessage = context.getString(R.string.error_generic))
                }
            }
        }
    }

    fun purchasePro(activity: Activity) {
        proPurchaseManager.launchPurchaseFlow(activity)
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

    /** Exports all reading data as CSV files to the Downloads folder via FileExportRepository. */
    fun exportData() {
        viewModelScope.launch {
            try {
                val csvFiles = exportDataUseCase.exportAllCsv()
                fileExportRepository.exportToDownloads(csvFiles)

                _uiState.update {
                    it.copy(
                    exportStatus = context.getString(R.string.settings_export_success)
                )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                    exportStatus = context.getString(R.string.settings_export_error)
                )
                }
            }
        }
    }

    /** Clears the export status message after it has been shown. */
    fun clearExportStatus() {
        _uiState.update { it.copy(exportStatus = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
