package com.devicepulse.domain.usecase

import com.devicepulse.data.network.NetworkRepository
import com.devicepulse.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkStateUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Flow<NetworkState> = networkRepository.getNetworkState()
}
