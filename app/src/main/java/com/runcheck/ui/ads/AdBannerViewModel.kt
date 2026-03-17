package com.runcheck.ui.ads

import androidx.lifecycle.ViewModel
import com.runcheck.pro.ProManager
import com.runcheck.pro.ProState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AdBannerViewModel @Inject constructor(
    proManager: ProManager
) : ViewModel() {
    val proState: StateFlow<ProState> = proManager.proState
}
