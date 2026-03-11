package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.NetworkRepository
import com.devicepulse.domain.repository.ProStatusProvider
import com.devicepulse.domain.repository.StorageRepository
import com.devicepulse.domain.repository.ThermalRepository
import com.devicepulse.domain.repository.ThrottlingRepository
import javax.inject.Inject

class CleanupOldReadingsUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val throttlingRepository: ThrottlingRepository,
    private val proStatusProvider: ProStatusProvider
) {
    suspend operator fun invoke() {
        if (proStatusProvider.isPro()) return

        val cutoff = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS
        batteryRepository.deleteOlderThan(cutoff)
        networkRepository.deleteOlderThan(cutoff)
        thermalRepository.deleteOlderThan(cutoff)
        storageRepository.deleteOlderThan(cutoff)
        throttlingRepository.deleteOlderThan(cutoff)
    }

    companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }
}
