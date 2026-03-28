package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.ScreenUsageStats
import com.runcheck.domain.model.SleepAnalysis
import com.runcheck.domain.repository.ScreenStateRepository
import javax.inject.Inject

class BatteryScreenInsightsUseCase
    @Inject
    constructor(
        private val screenStateRepository: ScreenStateRepository,
    ) {
        fun updateChargingStatus(chargingStatus: ChargingStatus) {
            screenStateRepository.updateChargingStatus(chargingStatus)
        }

        fun getScreenUsageStats(): ScreenUsageStats? = screenStateRepository.getScreenUsageStats()

        fun getSleepAnalysis(): SleepAnalysis? = screenStateRepository.getSleepAnalysis()
    }
