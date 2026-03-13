package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSpeedTestHistoryUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository
) {
    operator fun invoke(limit: Int): Flow<List<SpeedTestResult>> =
        speedTestRepository.getRecentResults(limit)

    fun getLatest(): Flow<SpeedTestResult?> =
        speedTestRepository.getLatestResult()
}
