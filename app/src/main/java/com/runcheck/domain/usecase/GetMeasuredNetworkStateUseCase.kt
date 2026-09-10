package com.runcheck.domain.usecase

import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val LATENCY_INTERVAL_MS = 30_000L

private data class TypedLatency(
    val networkKey: NetworkKey,
    val latencyMs: Int?,
)

private data class NetworkKey(
    val connectionType: ConnectionType,
    val defaultNetworkHandle: Long?,
)

class GetMeasuredNetworkStateUseCase
    @Inject
    constructor(
        private val getNetworkStateUseCase: GetNetworkStateUseCase,
        private val networkRepository: NetworkRepository,
    ) {
        @OptIn(ExperimentalCoroutinesApi::class)
        operator fun invoke(): Flow<NetworkState> {
            val networkStateFlow = getNetworkStateUseCase()

            // Re-trigger latency measurement when the active default network changes.
            val latencyFlow =
                networkStateFlow
                    .map { NetworkKey(it.connectionType, it.defaultNetworkHandle) }
                    .distinctUntilChanged()
                    .flatMapLatest { networkKey ->
                        flow {
                            emit(TypedLatency(networkKey, null))
                            if (networkKey.connectionType == ConnectionType.NONE) {
                                return@flow
                            }
                            // Measure immediately, then repeat periodically
                            while (true) {
                                val latency =
                                    try {
                                        networkRepository.measureLatency(networkKey.defaultNetworkHandle)
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (_: Exception) {
                                        null
                                    }
                                emit(TypedLatency(networkKey, latency))
                                kotlinx.coroutines.delay(LATENCY_INTERVAL_MS)
                            }
                        }
                    }

            return combine(networkStateFlow, latencyFlow) { state, typedLatency ->
                val currentNetworkKey = NetworkKey(state.connectionType, state.defaultNetworkHandle)
                val latency = typedLatency.latencyMs.takeIf { typedLatency.networkKey == currentNetworkKey }
                state.copy(latencyMs = latency)
            }
        }
    }
