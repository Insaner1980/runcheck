package com.runcheck.pro

import com.runcheck.billing.ProPurchaseManager
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProManager
    @Inject
    constructor(
        private val proPurchaseManager: ProPurchaseManager,
        private val dispatchers: AppDispatchers,
    ) : ProStatusProvider,
        ProStateProvider {
        private val scope = CoroutineScope(SupervisorJob() + dispatchers.mainImmediate)

        private val _proState = MutableStateFlow(ProState())
        override val proState: StateFlow<ProState> = _proState.asStateFlow()
        private val _isProStatusReady = MutableStateFlow(false)
        override val proAccessReady: StateFlow<Boolean> = _isProStatusReady.asStateFlow()
        override val isProStatusReady: Boolean
            get() = _isProStatusReady.value
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
                    proPurchaseManager.awaitPurchaseStatusReady()
                    proPurchaseManager.isProUser.collect { isPurchased ->
                        _proState.value =
                            if (isPurchased) {
                                ProState(
                                    status = ProStatus.PRO_PURCHASED,
                                    purchaseTimestamp = System.currentTimeMillis(),
                                )
                            } else {
                                ProState()
                            }
                        _isProStatusReady.value = true
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
