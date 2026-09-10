package com.runcheck.domain.usecase

import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.SpeedTestRepository
import javax.inject.Inject

private const val PRO_HISTORY_LIMIT = 100

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
            val historyLimit = if (proStatusProvider.isPro()) PRO_HISTORY_LIMIT else freeHistoryLimit
            speedTestRepository.saveResultAndTrim(result, historyLimit)
        }
    }
