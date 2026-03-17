package com.runcheck.ui.pro

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runcheck.billing.ProPurchaseManager
import com.runcheck.pro.ProFeature
import com.runcheck.pro.ProManager
import com.runcheck.pro.ProState
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
    val features: List<ProFeature> = ProFeature.entries
)

@HiltViewModel
class ProUpgradeViewModel @Inject constructor(
    private val proManager: ProManager,
    private val proPurchaseManager: ProPurchaseManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProUpgradeUiState())
    val uiState: StateFlow<ProUpgradeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            proManager.proState.collect { proState ->
                val wasPurchased = !_uiState.value.proState.isPro
                _uiState.update {
                    it.copy(
                        proState = proState,
                        purchaseCompleted = wasPurchased && proState.isPro &&
                            it.proState != ProState()
                    )
                }
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
    }

    fun purchasePro(activity: Activity) {
        if (!_uiState.value.billingAvailable) return
        proPurchaseManager.launchPurchaseFlow(activity)
    }

    fun dismissThankYou() {
        _uiState.update { it.copy(purchaseCompleted = false) }
    }
}
