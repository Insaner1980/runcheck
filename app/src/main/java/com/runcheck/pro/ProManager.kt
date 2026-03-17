package com.runcheck.pro

import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProManager @Inject constructor(
    private val trialManager: TrialManager,
    private val proStatusProvider: ProStatusProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _proState = MutableStateFlow(ProState())
    val proState: StateFlow<ProState> = _proState.asStateFlow()

    val proStateFlow: Flow<ProState> = _proState.asStateFlow()

    fun initialize() {
        scope.launch {
            val isFirstLaunch = trialManager.initialize()

            combine(
                trialManager.trialState,
                proStatusProvider.isProUser
            ) { trial, isPurchased ->
                when {
                    isPurchased -> ProState(
                        status = ProStatus.PRO_PURCHASED,
                        trialDaysRemaining = 0,
                        trialStartTimestamp = trial.startTimestamp,
                        purchaseTimestamp = System.currentTimeMillis()
                    )
                    trial.isActive -> ProState(
                        status = ProStatus.TRIAL_ACTIVE,
                        trialDaysRemaining = trial.daysRemaining,
                        trialStartTimestamp = trial.startTimestamp
                    )
                    else -> ProState(
                        status = ProStatus.TRIAL_EXPIRED,
                        trialDaysRemaining = 0,
                        trialStartTimestamp = trial.startTimestamp
                    )
                }
            }.collect { state ->
                _proState.value = state
            }
        }
    }

    fun hasFeature(feature: ProFeature): Boolean = _proState.value.hasFeature(feature)

    fun isPro(): Boolean = _proState.value.isPro
}
