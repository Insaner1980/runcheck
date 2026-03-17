package com.runcheck.ui.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.domain.usecase.ObserveSettingsUseCase
import com.runcheck.domain.usecase.SetCrashReportingEnabledUseCase
import com.runcheck.domain.usecase.SetDataRetentionUseCase
import com.runcheck.domain.usecase.SetMonitoringIntervalUseCase
import com.runcheck.domain.usecase.SetNotificationsEnabledUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    @param:ApplicationContext private val context: Context,
    private val observeSettings: ObserveSettingsUseCase,
    private val proPurchaseManager: ProPurchaseManager,
    private val proStatusProvider: ProStatusProvider,
    private val exportDataUseCase: ExportDataUseCase,
    private val setDataRetentionUseCase: SetDataRetentionUseCase,
    private val setMonitoringIntervalUseCase: SetMonitoringIntervalUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val setCrashReportingEnabledUseCase: SetCrashReportingEnabledUseCase,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                observeSettings(),
                proPurchaseManager.isProUser,
                proPurchaseManager.billingAvailable
            ) { settings, isPro, billingAvailable ->
                Triple(settings, isPro, billingAvailable)
            }
                .catch {
                    _uiState.update { current ->
                        current.copy(errorMessage = context.getString(R.string.common_error_generic))
                    }
                }
                .collect { (settings, isPro, billingAvailable) ->
                    _uiState.update { current ->
                        current.copy(
                            preferences = settings.preferences,
                            deviceProfile = settings.deviceProfile,
                            isPro = isPro,
                            billingAvailable = billingAvailable
                        )
                    }
                }
        }
        viewModelScope.launch {
            try {
                proPurchaseManager.getFormattedPrice()?.let { price ->
                    _uiState.update { current -> current.copy(proPrice = price) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(errorMessage = context.getString(R.string.common_error_generic))
                }
            }
        }
    }

    fun purchasePro(activity: Activity) {
        if (!_uiState.value.billingAvailable) {
            _uiState.update {
                it.copy(billingStatus = context.getString(R.string.settings_billing_unavailable))
            }
            return
        }
        proPurchaseManager.launchPurchaseFlow(activity)
    }

    fun refreshPurchaseStatus() {
        viewModelScope.launch {
            try {
                val statusMessage = when (proPurchaseManager.refreshPurchaseStatus()) {
                    ProPurchaseRefreshResult.ACTIVE -> context.getString(R.string.settings_restore_success)
                    ProPurchaseRefreshResult.NOT_ACTIVE -> context.getString(R.string.settings_restore_not_found)
                    ProPurchaseRefreshResult.UNAVAILABLE -> context.getString(R.string.settings_restore_unavailable)
                }
                _uiState.update { current -> current.copy(billingStatus = statusMessage) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(billingStatus = context.getString(R.string.common_error_generic))
                }
            }
        }
    }

    fun setMonitoringInterval(interval: MonitoringInterval) {
        viewModelScope.launch {
            try {
                setMonitoringIntervalUseCase(interval)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.common_error_generic)) }
            }
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                setNotificationsEnabledUseCase(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.common_error_generic)) }
            }
        }
    }

    fun setDataRetention(retention: DataRetention) {
        viewModelScope.launch {
            try {
                setDataRetentionUseCase(retention)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.common_error_generic)) }
            }
        }
    }

    fun setCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            try {
                setCrashReportingEnabledUseCase(enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = context.getString(R.string.common_error_generic)) }
            }
        }
    }

    /** Exports all reading data as CSV files to the Downloads folder via FileExportRepository. */
    fun exportData() {
        if (!proStatusProvider.isPro()) {
            _uiState.update {
                it.copy(errorMessage = context.getString(R.string.pro_feature_locked_generic))
            }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true, exportStatus = null) }
                val exportUris = exportDataUseCase.prepareExportShare()
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportUris = exportUris,
                        exportStatus = if (exportUris.isNotEmpty()) {
                            context.getString(R.string.settings_export_ready)
                        } else {
                            context.getString(R.string.settings_export_error)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
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

    fun clearExportUris() {
        _uiState.update { it.copy(exportUris = null) }
    }

    fun clearBillingStatus() {
        _uiState.update { it.copy(billingStatus = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── New settings handlers ──────────────────────────────────────────

    fun setNotifLowBattery(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotifLowBattery(enabled) }
    }

    fun setNotifHighTemp(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotifHighTemp(enabled) }
    }

    fun setNotifLowStorage(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotifLowStorage(enabled) }
    }

    fun setNotifChargeComplete(enabled: Boolean) {
        viewModelScope.launch { userPreferencesRepository.setNotifChargeComplete(enabled) }
    }

    fun setAlertBatteryThreshold(value: Int) {
        viewModelScope.launch { userPreferencesRepository.setAlertBatteryThreshold(value) }
    }

    fun setAlertTempThreshold(value: Int) {
        viewModelScope.launch { userPreferencesRepository.setAlertTempThreshold(value) }
    }

    fun setAlertStorageThreshold(value: Int) {
        viewModelScope.launch { userPreferencesRepository.setAlertStorageThreshold(value) }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { userPreferencesRepository.setTemperatureUnit(unit) }
    }
}
