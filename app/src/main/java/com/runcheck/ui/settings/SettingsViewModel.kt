package com.runcheck.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.billing.ProPurchaseRefreshResult
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.PurchaseEvent
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.AppLanguage
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.usecase.ClearMonitoringDataUseCase
import com.runcheck.domain.usecase.ExportDataUseCase
import com.runcheck.domain.usecase.IsProUserUseCase
import com.runcheck.domain.usecase.ManageInfoCardDismissalsUseCase
import com.runcheck.domain.usecase.ManageUserPreferencesUseCase
import com.runcheck.domain.usecase.ObserveSettingsUseCase
import com.runcheck.domain.usecase.SetCrashReportingEnabledUseCase
import com.runcheck.domain.usecase.SetDataRetentionUseCase
import com.runcheck.domain.usecase.SetMonitoringIntervalUseCase
import com.runcheck.domain.usecase.SetNotificationsEnabledUseCase
import com.runcheck.ui.common.UiText
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
    private val observeSettings: ObserveSettingsUseCase,
    private val proPurchaseManager: ProPurchaseManager,
    private val isProUser: IsProUserUseCase,
    private val clearMonitoringDataUseCase: ClearMonitoringDataUseCase,
    private val exportDataUseCase: ExportDataUseCase,
    private val setDataRetentionUseCase: SetDataRetentionUseCase,
    private val setMonitoringIntervalUseCase: SetMonitoringIntervalUseCase,
    private val setNotificationsEnabledUseCase: SetNotificationsEnabledUseCase,
    private val setCrashReportingEnabledUseCase: SetCrashReportingEnabledUseCase,
    private val manageUserPreferences: ManageUserPreferencesUseCase,
    private val manageInfoCardDismissals: ManageInfoCardDismissalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private var isFetchingPrice = false

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
                        current.copy(errorMessage = UiText.Resource(R.string.common_error_generic))
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
                    if (billingAvailable) {
                        fetchProPriceIfNeeded()
                    }
                }
        }
        fetchProPriceIfNeeded(force = true)
        viewModelScope.launch {
            proPurchaseManager.purchaseEvents.collect { event ->
                when (event) {
                    is PurchaseEvent.Pending -> _uiState.update {
                        it.copy(billingStatus = UiText.Resource(R.string.billing_purchase_pending))
                    }
                    is PurchaseEvent.Error -> _uiState.update {
                        it.copy(billingStatus = UiText.Resource(R.string.billing_purchase_error))
                    }
                    is PurchaseEvent.AlreadyOwned -> _uiState.update {
                        it.copy(billingStatus = UiText.Resource(R.string.billing_already_owned))
                    }
                    is PurchaseEvent.Canceled,
                    is PurchaseEvent.Success -> {
                        _uiState.update { it.copy(billingStatus = null) }
                    }
                }
            }
        }
    }

    fun purchasePro(activity: Activity) {
        if (!_uiState.value.billingAvailable) {
            _uiState.update {
                it.copy(billingStatus = UiText.Resource(R.string.settings_billing_unavailable))
            }
            return
        }
        proPurchaseManager.launchPurchaseFlow(activity)
    }

    fun refreshPurchaseStatus() {
        viewModelScope.launch {
            try {
                val statusMessage = when (proPurchaseManager.refreshPurchaseStatus()) {
                    ProPurchaseRefreshResult.ACTIVE -> UiText.Resource(R.string.settings_restore_success)
                    ProPurchaseRefreshResult.NOT_ACTIVE -> UiText.Resource(R.string.settings_restore_not_found)
                    ProPurchaseRefreshResult.UNAVAILABLE -> UiText.Resource(R.string.settings_restore_unavailable)
                }
                _uiState.update { current -> current.copy(billingStatus = statusMessage) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(billingStatus = UiText.Resource(R.string.common_error_generic))
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
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
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
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
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
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
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
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
            }
        }
    }

    fun exportData() {
        if (!isProUser()) {
            _uiState.update {
                it.copy(errorMessage = UiText.Resource(R.string.pro_feature_locked_generic))
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
                            UiText.Resource(R.string.settings_export_ready)
                        } else {
                            UiText.Resource(R.string.settings_export_error)
                        }
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportStatus = UiText.Resource(R.string.settings_export_error)
                    )
                }
            }
        }
    }

    fun clearExportStatus() {
        _uiState.update { it.copy(exportStatus = null) }
    }

    fun clearExportUris() {
        _uiState.update { it.copy(exportUris = null) }
    }

    fun clearClearDataStatus() {
        _uiState.update { it.copy(clearDataStatus = null) }
    }

    fun clearBillingStatus() {
        _uiState.update { it.copy(billingStatus = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // ── New settings handlers ──────────────────────────────────────────

    fun setNotifLowBattery(enabled: Boolean) {
        executePreferenceUpdate {
            manageUserPreferences.setNotifLowBattery(enabled)
        }
    }

    fun setNotifHighTemp(enabled: Boolean) {
        executePreferenceUpdate {
            manageUserPreferences.setNotifHighTemp(enabled)
        }
    }

    fun setNotifLowStorage(enabled: Boolean) {
        executePreferenceUpdate {
            manageUserPreferences.setNotifLowStorage(enabled)
        }
    }

    fun setNotifChargeComplete(enabled: Boolean) {
        executePreferenceUpdate {
            manageUserPreferences.setNotifChargeComplete(enabled)
        }
    }

    fun setAlertBatteryThreshold(value: Int) {
        executePreferenceUpdate {
            manageUserPreferences.setAlertBatteryThreshold(value)
        }
    }

    fun setAlertTempThreshold(value: Int) {
        executePreferenceUpdate {
            manageUserPreferences.setAlertTempThreshold(value)
        }
    }

    fun setAlertStorageThreshold(value: Int) {
        executePreferenceUpdate {
            manageUserPreferences.setAlertStorageThreshold(value)
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        executePreferenceUpdate {
            manageUserPreferences.setTemperatureUnit(unit)
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        executePreferenceUpdate {
            manageUserPreferences.setAppLanguage(language)
        }
    }

    fun setLiveNotificationEnabled(enabled: Boolean) {
        executePreferenceUpdate {
            manageUserPreferences.setLiveNotificationEnabled(enabled)
        }
    }

    fun setLiveNotifCurrent(enabled: Boolean) {
        executePreferenceUpdate { manageUserPreferences.setLiveNotifCurrent(enabled) }
    }

    fun setLiveNotifDrainRate(enabled: Boolean) {
        executePreferenceUpdate { manageUserPreferences.setLiveNotifDrainRate(enabled) }
    }

    fun setLiveNotifTemperature(enabled: Boolean) {
        executePreferenceUpdate { manageUserPreferences.setLiveNotifTemperature(enabled) }
    }

    fun setLiveNotifScreenStats(enabled: Boolean) {
        executePreferenceUpdate { manageUserPreferences.setLiveNotifScreenStats(enabled) }
    }

    fun setLiveNotifRemainingTime(enabled: Boolean) {
        executePreferenceUpdate { manageUserPreferences.setLiveNotifRemainingTime(enabled) }
    }

    fun resetTips() {
        executePreferenceUpdate {
            manageInfoCardDismissals.resetDismissedCards()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                clearMonitoringDataUseCase()
                _uiState.update {
                    it.copy(clearDataStatus = UiText.Resource(R.string.settings_data_cleared))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
            }
        }
    }

    private fun executePreferenceUpdate(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = UiText.Resource(R.string.common_error_generic)) }
            }
        }
    }

    private fun fetchProPriceIfNeeded(force: Boolean = false) {
        if (isFetchingPrice || (!force && _uiState.value.proPrice != null)) {
            return
        }
        viewModelScope.launch {
            isFetchingPrice = true
            try {
                proPurchaseManager.getFormattedPrice()?.let { price ->
                    _uiState.update { current -> current.copy(proPrice = price) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { current ->
                    current.copy(errorMessage = UiText.Resource(R.string.common_error_generic))
                }
            } finally {
                isFetchingPrice = false
            }
        }
    }
}
