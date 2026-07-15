package com.runcheck.domain.usecase

import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkRepository
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
    val connectionType: ConnectionType,
    val latencyMs: Int?,
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

            // Re-trigger latency measurement when connection type changes
            val latencyFlow =
                networkStateFlow
                    .map { it.connectionType }
                    .distinctUntilChanged()
                    .flatMapLatest { connectionType ->
                        flow {
                            emit(TypedLatency(connectionType, null))
                            if (connectionType == ConnectionType.NONE) {
                                return@flow
                            }
                            // Measure immediately, then repeat periodically
                            while (true) {
                                val latency =
                                    try {
                                        networkRepository.measureLatency()
                                    } catch (_: Exception) {
                                        null
                                    }
                                emit(TypedLatency(connectionType, latency))
                                kotlinx.coroutines.delay(LATENCY_INTERVAL_MS)
                            }
                        }
                    }

            return combine(networkStateFlow, latencyFlow) { state, typedLatency ->
                val latency = typedLatency.latencyMs.takeIf { typedLatency.connectionType == state.connectionType }
                state.copy(latencyMs = latency)
            }
        }
    }
