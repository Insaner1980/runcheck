package com.runcheck.domain.usecase

import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.repository.SpeedTestRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RunSpeedTestUseCase @Inject constructor(
    private val speedTestRepository: SpeedTestRepository
) {
    operator fun invoke(allowCellular: Boolean = false): Flow<SpeedTestProgress> =
        speedTestRepository.runSpeedTest(allowCellular = allowCellular)
}
