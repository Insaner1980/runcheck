package com.runcheck.domain.usecase

import com.runcheck.domain.model.SpeedTestHistoryPolicy
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
        suspend operator fun invoke(result: SpeedTestResult) {
            val historyLimit = SpeedTestHistoryPolicy.resultLimit(isPro = proStatusProvider.isPro())
            speedTestRepository.saveResultAndTrim(result, historyLimit)
        }
    }
