package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetBatteryHistoryUseCase
    @Inject
    constructor(
        private val batteryRepository: BatteryRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<BatteryReading>> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    flow {
                        val since =
                            when {
                                !isPro -> {
                                    System.currentTimeMillis() - HistoryPeriod.DAY.durationMs
                                }

                                period == HistoryPeriod.SINCE_UNPLUG -> {
                                    batteryRepository.getLastChargingTimestamp() ?: 0L
                                }

                                period == HistoryPeriod.ALL -> {
                                    0L
                                }

                                else -> {
                                    System.currentTimeMillis() - period.durationMs
                                }
                            }
                        val limit = if (period == HistoryPeriod.ALL) MAX_HISTORY_POINTS else null
                        emitAll(batteryRepository.getReadingsSince(since, limit))
                    }
                }

        companion object {
            private const val MAX_HISTORY_POINTS = 5_000
        }
    }
