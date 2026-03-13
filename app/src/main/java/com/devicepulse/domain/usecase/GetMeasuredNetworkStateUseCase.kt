package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.repository.NetworkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetMeasuredNetworkStateUseCase @Inject constructor(
    private val getNetworkStateUseCase: GetNetworkStateUseCase,
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Flow<NetworkState> =
        getNetworkStateUseCase().mapLatest { state ->
            val latencyMs = if (state.connectionType == ConnectionType.NONE) {
                null
            } else {
                runCatching { networkRepository.measureLatency() }.getOrNull()
            }
            state.copy(latencyMs = latencyMs)
        }
}
