package com.devicepulse.domain.usecase

import com.devicepulse.data.battery.BatteryRepository
import com.devicepulse.data.billing.ProStatusRepository
import com.devicepulse.data.db.entity.BatteryReadingEntity
import com.devicepulse.domain.model.HistoryPeriod
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBatteryHistoryUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val proStatusRepository: ProStatusRepository
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<BatteryReadingEntity>> {
        val since = when {
            !proStatusRepository.isPro() -> {
                // Free users always get 24h only
                System.currentTimeMillis() - HistoryPeriod.DAY.durationMs
            }
            period == HistoryPeriod.ALL -> 0L
            else -> System.currentTimeMillis() - period.durationMs
        }
        return batteryRepository.getReadingsSince(since)
    }
}
