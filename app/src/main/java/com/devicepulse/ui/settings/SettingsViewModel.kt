package com.devicepulse.ui.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devicepulse.R
import com.devicepulse.billing.ProPurchaseRefreshResult
import com.devicepulse.billing.ProPurchaseManager
import com.devicepulse.domain.model.DataRetention
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.model.MonitoringInterval
import com.devicepulse.domain.usecase.ExportDataUseCase
import com.devicepulse.domain.usecase.ObserveSettingsUseCase
import com.devicepulse.domain.usecase.SetCrashReportingEnabledUseCase
import com.devicepulse.domain.usecase.SetDataRetentionUseCase
import com.devicepulse.domain.usecase.SetMonitoringIntervalUseCase
import com.devicepulse.domain.usecase.SetNotificationsEnabledUseCase
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
    @param:ApplicationContext private val context: Context,
    private val observeSettings: ObserveSettingsUseCase,
    private val proPurchaseManager: ProPurchaseManager,
    private val proStatusProvider: ProStatusProvider,
    private val exportDataUseCase: ExportDataUseCase,
    private val setDataRetentionUseCase: SetDataRetentionUseCase,
    private val setMonitoringIntervalUseCase: SetMonitoringIntervalUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val setCrashReportingEnabledUseCase: SetCrashReportingEnabledUseCase
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
                        current.copy(errorMessage = context.getString(R.string.error_generic))
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
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(errorMessage = context.getString(R.string.error_generic))
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
            val statusMessage = when (proPurchaseManager.refreshPurchaseStatus()) {
                ProPurchaseRefreshResult.ACTIVE -> context.getString(R.string.settings_restore_success)
                ProPurchaseRefreshResult.NOT_ACTIVE -> context.getString(R.string.settings_restore_not_found)
                ProPurchaseRefreshResult.UNAVAILABLE -> context.getString(R.string.settings_restore_unavailable)
            }
            _uiState.update { current -> current.copy(billingStatus = statusMessage) }
        }
    }

    fun setMonitoringInterval(interval: MonitoringInterval) {
        viewModelScope.launch {
            setMonitoringIntervalUseCase(interval)
        }
    }

    fun setNotifications(enabled: Boolean) {
        viewModelScope.launch { setNotificationsEnabledUseCase(enabled) }
    }

    fun setDataRetention(retention: DataRetention) {
        viewModelScope.launch {
            setDataRetentionUseCase(retention)
        }
    }

    fun setCrashReporting(enabled: Boolean) {
        viewModelScope.launch {
            setCrashReportingEnabledUseCase(enabled)
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
                _uiState.update {
                    it.copy(
                        exportStatus = if (exportDataUseCase.exportAllToDownloads()) {
                            context.getString(R.string.settings_export_success)
                        } else {
                            context.getString(R.string.settings_export_error)
                        }
                    )
                }
            } catch (_: Exception) {
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

    fun clearBillingStatus() {
        _uiState.update { it.copy(billingStatus = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
