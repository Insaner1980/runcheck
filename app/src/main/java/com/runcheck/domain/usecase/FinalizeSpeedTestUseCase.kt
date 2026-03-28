package com.runcheck.domain.usecase

import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.SpeedTestRepository
import javax.inject.Inject

class FinalizeSpeedTestUseCase
    @Inject
    constructor(
        private val speedTestRepository: SpeedTestRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        suspend operator fun invoke(
            result: SpeedTestResult,
            freeHistoryLimit: Int,
        ) {
            speedTestRepository.saveResult(result)
            val maxHistory = if (proStatusProvider.isPro()) Int.MAX_VALUE else freeHistoryLimit
            speedTestRepository.trimResults(maxHistory)
        }
    }
