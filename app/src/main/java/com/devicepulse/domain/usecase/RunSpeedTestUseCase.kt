package com.devicepulse.domain.usecase

import com.devicepulse.data.network.SpeedTestRepository
import com.devicepulse.data.network.SpeedTestService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunSpeedTestUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository
) {
    operator fun invoke(): Flow<SpeedTestService.SpeedTestProgress> =
        speedTestRepository.runSpeedTest()
}
