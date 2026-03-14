package com.runcheck.domain.usecase

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.AppBatteryUsageRepository
import com.runcheck.domain.repository.SpeedTestRepository
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.ThrottlingRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CleanupOldReadingsUseCase @Inject constructor(
    private val transactionRunner: DatabaseTransactionRunner,
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val throttlingRepository: ThrottlingRepository,
    private val appBatteryUsageRepository: AppBatteryUsageRepository,
    private val speedTestRepository: SpeedTestRepository,
    private val proStatusProvider: ProStatusProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    suspend operator fun invoke() {
        val cutoff = resolveCutoff(System.currentTimeMillis()) ?: return

        transactionRunner.runInTransaction {
            batteryRepository.deleteOlderThan(cutoff)
            networkRepository.deleteOlderThan(cutoff)
            thermalRepository.deleteOlderThan(cutoff)
            storageRepository.deleteOlderThan(cutoff)
            throttlingRepository.deleteOlderThan(cutoff)
            appBatteryUsageRepository.deleteOlderThan(cutoff)
            speedTestRepository.deleteOlderThan(cutoff)
        }
    }

    private suspend fun resolveCutoff(now: Long): Long? {
        if (!proStatusProvider.isPro()) {
            return now - TWENTY_FOUR_HOURS_MS
        }

        val retention = userPreferencesRepository.getPreferences().first().dataRetention
        return retention.durationMillis?.let { now - it }
    }

    companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }
}
