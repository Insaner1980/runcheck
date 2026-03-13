package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkStateUseCase @Inject constructor(
    private val networkRepository: NetworkRepository
) {
    operator fun invoke(): Flow<NetworkState> = networkRepository.getNetworkState()
}
