package com.runcheck.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProManager
    @Inject
    constructor(
        private val trialManager: TrialManager,
        private val proPurchaseManager: ProPurchaseManager,
        private val dispatchers: AppDispatchers,
    ) : ProStatusProvider,
        ProStateProvider {
        private val scope = CoroutineScope(SupervisorJob() + dispatchers.mainImmediate)
        private val trialExpiryScope = CoroutineScope(SupervisorJob() + dispatchers.default)

        private val _proState = MutableStateFlow(ProState())
        override val proState: StateFlow<ProState> = _proState.asStateFlow()
        private val _isProStatusReady = MutableStateFlow(false)
        override val isProStatusReady: Boolean
            get() = _isProStatusReady.value
        override val isProUser: Flow<Boolean> =
            proState
                .map { it.isPro }
                .distinctUntilChanged()

        private var initialized = false
        private var trialExpiryJob: Job? = null

        @Suppress("TooGenericExceptionCaught")
        fun initialize() {
            if (initialized) return
            initialized = true
            scope.launch {
                try {
                    trialManager.initialize()
                    proPurchaseManager.awaitPurchaseStatusReady()

                    combine(
                        trialManager.trialState,
                        proPurchaseManager.isProUser,
                    ) { trial, isPurchased ->
                        val trialIsActive =
                            trial.isActive &&
                                isTrialWithinPeriod(trial.startTimestamp, System.currentTimeMillis())
                        when {
                            isPurchased -> {
                                ProState(
                                    status = ProStatus.PRO_PURCHASED,
                                    trialDaysRemaining = 0,
                                    trialStartTimestamp = trial.startTimestamp,
                                    purchaseTimestamp = System.currentTimeMillis(),
                                )
                            }

                            trialIsActive -> {
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
                        _isProStatusReady.value = true
                        scheduleTrialExpiryRefresh(state)
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

        private fun scheduleTrialExpiryRefresh(state: ProState) {
            trialExpiryJob?.cancel()
            if (state.status != ProStatus.TRIAL_ACTIVE || state.trialStartTimestamp <= 0L) return

            trialExpiryJob =
                trialExpiryScope.launch {
                    delay(trialExpiryRefreshDelayMs(state.trialStartTimestamp, System.currentTimeMillis()))
                    val current = _proState.value
                    if (current.status == ProStatus.TRIAL_ACTIVE &&
                        current.trialStartTimestamp == state.trialStartTimestamp
                    ) {
                        _proState.value = current.copy(status = ProStatus.TRIAL_EXPIRED, trialDaysRemaining = 0)
                    }
                }
        }

        private companion object {
            private const val TAG = "ProManager"
        }
    }

internal fun trialExpiryRefreshDelayMs(
    trialStartTimestamp: Long,
    now: Long,
): Long {
    val expiresAt = trialExpiresAtMs(trialStartTimestamp)
    return (expiresAt - now).coerceAtLeast(0L) + TRIAL_EXPIRY_REFRESH_GRACE_MS
}

private fun isTrialWithinPeriod(
    trialStartTimestamp: Long,
    now: Long,
): Boolean = trialStartTimestamp > 0L && now < trialExpiresAtMs(trialStartTimestamp)

private fun trialExpiresAtMs(trialStartTimestamp: Long): Long =
    trialStartTimestamp + TimeUnit.DAYS.toMillis(TrialManager.TRIAL_DURATION_DAYS.toLong())

private const val TRIAL_EXPIRY_REFRESH_GRACE_MS = 1_000L
