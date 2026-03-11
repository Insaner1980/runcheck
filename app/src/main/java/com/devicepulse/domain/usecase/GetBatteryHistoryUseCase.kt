package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.BatteryReading
import com.devicepulse.domain.model.HistoryPeriod
import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBatteryHistoryUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val proStatusProvider: ProStatusProvider
) {
    operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<BatteryReading>> {
        val since = when {
            !proStatusProvider.isPro() -> {
                System.currentTimeMillis() - HistoryPeriod.DAY.durationMs
            }
            period == HistoryPeriod.ALL -> 0L
            else -> System.currentTimeMillis() - period.durationMs
        }
        return batteryRepository.getReadingsSince(since)
    }
}
