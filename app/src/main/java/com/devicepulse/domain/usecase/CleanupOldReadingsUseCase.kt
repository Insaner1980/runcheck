package com.devicepulse.domain.usecase

import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.db.dao.BatteryReadingDao
import com.devicepulse.data.db.dao.NetworkReadingDao
import com.devicepulse.data.db.dao.StorageReadingDao
import com.devicepulse.data.db.dao.ThermalReadingDao
import javax.inject.Inject

class CleanupOldReadingsUseCase @Inject constructor(
    private val batteryReadingDao: BatteryReadingDao,
    private val networkReadingDao: NetworkReadingDao,
    private val thermalReadingDao: ThermalReadingDao,
    private val storageReadingDao: StorageReadingDao,
    private val proStatusRepository: ProStatusRepository
) {
    suspend operator fun invoke() {
        if (proStatusRepository.isPro()) return

        val cutoff = System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS
        batteryReadingDao.deleteOlderThan(cutoff)
        networkReadingDao.deleteOlderThan(cutoff)
        thermalReadingDao.deleteOlderThan(cutoff)
        storageReadingDao.deleteOlderThan(cutoff)
    }

    companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }
}
