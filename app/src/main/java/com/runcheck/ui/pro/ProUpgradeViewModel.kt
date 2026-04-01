package com.runcheck.ui.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.R
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.billing.PurchaseEvent
import com.runcheck.pro.ProFeature
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStateProvider
import com.runcheck.ui.common.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProUpgradeUiState(
    val proState: ProState = ProState(),
    val formattedPrice: String? = null,
    val billingAvailable: Boolean = false,
    val purchaseCompleted: Boolean = false,
    val purchasePending: Boolean = false,
    val purchaseError: UiText? = null,
    val features: List<ProFeature> = ProFeature.entries,
)

@HiltViewModel
class ProUpgradeViewModel
    @Inject
    constructor(
        private val proStateProvider: ProStateProvider,
        private val proPurchaseManager: ProPurchaseManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProUpgradeUiState())
        val uiState: StateFlow<ProUpgradeUiState> = _uiState.asStateFlow()

        private var initialLoadDone = false

        init {
            viewModelScope.launch {
                proStateProvider.proState.collect { proState ->
                    val wasNotPro = !_uiState.value.proState.isPro
                    _uiState.update {
                        it.copy(
                            proState = proState,
                            purchaseCompleted = initialLoadDone && wasNotPro && proState.isPro,
                            purchasePending = false,
                        )
                    }
                    initialLoadDone = true
                }
            }
            viewModelScope.launch {
                proPurchaseManager.billingAvailable.collect { available ->
                    _uiState.update { it.copy(billingAvailable = available) }
                }
            }
            viewModelScope.launch {
                try {
                    proPurchaseManager.getFormattedPrice()?.let { price ->
                        _uiState.update { it.copy(formattedPrice = price) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Price unavailable — button will show without price
                }
            }
            viewModelScope.launch {
                proPurchaseManager.purchaseEvents.collect { event ->
                    when (event) {
                        is PurchaseEvent.Pending -> {
                            _uiState.update {
                                it.copy(purchasePending = true, purchaseError = null)
                            }
                        }

                        is PurchaseEvent.Error -> {
                            _uiState.update {
                                it.copy(
                                    purchaseError = UiText.Dynamic(event.debugMessage),
                                    purchasePending = false,
                                )
                            }
                        }

                        is PurchaseEvent.AlreadyOwned -> {
                            _uiState.update {
                                it.copy(purchaseError = null, purchasePending = false)
                            }
                        }

                        is PurchaseEvent.Canceled,
                        is PurchaseEvent.Success,
                        -> {
                            // No action needed
                        }
                    }
                }
            }
        }

        fun purchasePro(activity: Activity) {
            if (!_uiState.value.billingAvailable) return
            _uiState.update { it.copy(purchaseError = null) }
            proPurchaseManager.launchPurchaseFlow(activity)
        }

        fun dismissThankYou() {
            _uiState.update { it.copy(purchaseCompleted = false) }
        }

        fun clearPurchaseError() {
            _uiState.update { it.copy(purchaseError = null) }
        }
    }
