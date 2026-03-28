package com.runcheck.domain.usecase

import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ThrottlingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetThrottlingHistoryUseCase
    @Inject
    constructor(
        private val throttlingRepository: ThrottlingRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(): Flow<List<ThrottlingEvent>> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    if (isPro) {
                        throttlingRepository.getRecentEvents()
                    } else {
                        flowOf(emptyList())
                    }
                }
    }
