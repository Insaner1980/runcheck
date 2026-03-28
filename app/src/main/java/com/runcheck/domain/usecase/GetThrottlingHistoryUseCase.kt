package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetThrottlingHistoryUseCase
    @Inject
    constructor(
        private val throttlingRepository: ThrottlingRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(): Flow<List<ThrottlingEvent>> =
            if (proStatusProvider.isPro()) {
                throttlingRepository.getRecentEvents()
            } else {
                flowOf(emptyList())
            }
    }
