package com.runcheck.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProManager
    @Inject
    constructor(
        private val trialManager: TrialManager,
        private val proPurchaseManager: ProPurchaseManager,
    ) : ProStatusProvider,
        ProStateProvider {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        private val _proState = MutableStateFlow(ProState())
        override val proState: StateFlow<ProState> = _proState.asStateFlow()
        override val isProUser: Flow<Boolean> =
            proState
                .map { it.isPro }
                .distinctUntilChanged()

        private var initialized = false

        @Suppress("TooGenericExceptionCaught")
        fun initialize() {
            if (initialized) return
            initialized = true
            scope.launch {
                try {
                    trialManager.initialize()

                    combine(
                        trialManager.trialState,
                        proPurchaseManager.isProUser,
                    ) { trial, isPurchased ->
                        when {
                            isPurchased -> {
                                ProState(
                                    status = ProStatus.PRO_PURCHASED,
                                    trialDaysRemaining = 0,
                                    trialStartTimestamp = trial.startTimestamp,
                                    purchaseTimestamp = System.currentTimeMillis(),
                                )
                            }

                            trial.isActive -> {
                                ProState(
                                    status = ProStatus.TRIAL_ACTIVE,
                                    trialDaysRemaining = trial.daysRemaining,
                                    trialStartTimestamp = trial.startTimestamp,
                                )
                            }

                            else -> {
                                ProState(
                                    status = ProStatus.TRIAL_EXPIRED,
                                    trialDaysRemaining = 0,
                                    trialStartTimestamp = trial.startTimestamp,
                                )
                            }
                        }
                    }.collect { state ->
                        val wasFree = _proState.value.status != ProStatus.PRO_PURCHASED
                        _proState.value = state
                        if (wasFree && state.status == ProStatus.PRO_PURCHASED) {
                            trialManager.cancelTrialNotifications()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ReleaseSafeLog.error(TAG, "Failed to observe pro state", e)
                }
            }
        }

        fun hasFeature(feature: ProFeature): Boolean = _proState.value.hasFeature(feature)

        override fun isPro(): Boolean = _proState.value.isPro

        private companion object {
            private const val TAG = "ProManager"
        }
    }
