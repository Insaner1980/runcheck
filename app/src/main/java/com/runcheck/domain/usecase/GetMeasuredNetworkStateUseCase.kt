package com.runcheck.domain.usecase

import com.runcheck.domain.model.NetworkState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMeasuredNetworkStateUseCase @Inject constructor(
    private val getNetworkStateUseCase: GetNetworkStateUseCase
) {
    operator fun invoke(): Flow<NetworkState> = getNetworkStateUseCase()
}
