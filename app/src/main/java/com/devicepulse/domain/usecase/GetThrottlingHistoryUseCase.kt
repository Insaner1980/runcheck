package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ThrottlingEvent
import com.devicepulse.domain.repository.ThrottlingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThrottlingHistoryUseCase @Inject constructor(
    private val throttlingRepository: ThrottlingRepository
) {
    operator fun invoke(): Flow<List<ThrottlingEvent>> =
        throttlingRepository.getRecentEvents()
}
