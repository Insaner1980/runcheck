package com.devicepulse.domain.usecase

import com.devicepulse.data.battery.BatteryRepository
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.db.entity.BatteryReadingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBatteryHistoryUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val proStatusRepository: ProStatusRepository
) {
    operator fun invoke(): Flow<List<BatteryReadingEntity>> {
        val since = if (proStatusRepository.isPro()) {
            0L // All time for pro users
        } else {
            System.currentTimeMillis() - TWENTY_FOUR_HOURS_MS
        }
        return batteryRepository.getReadingsSince(since)
    }

    companion object {
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }
}
