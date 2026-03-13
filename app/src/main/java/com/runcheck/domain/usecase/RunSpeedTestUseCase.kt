package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.SpeedTestProgress
import com.devicepulse.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunSpeedTestUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository
) {
    operator fun invoke(): Flow<SpeedTestProgress> =
        speedTestRepository.runSpeedTest()
}
