package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThermalHistoryUseCase
    @Inject
    constructor(
        private val thermalRepository: ThermalRepository,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<ThermalReading>> {
            val since =
                when (period) {
                    HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
                    else -> System.currentTimeMillis() - period.durationMs
                }
            val limit = if (period == HistoryPeriod.ALL) 5_000 else null
            return thermalRepository.getReadingsSince(since, limit)
        }
    }
