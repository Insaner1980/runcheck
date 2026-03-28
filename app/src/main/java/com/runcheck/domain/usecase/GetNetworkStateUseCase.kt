package com.runcheck.domain.usecase

import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNetworkStateUseCase
    @Inject
    constructor(
        private val networkRepository: NetworkRepository,
    ) {
        operator fun invoke(): Flow<NetworkState> = networkRepository.getNetworkState()
    }
