package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class GetThermalHistoryUseCase
    @Inject
    constructor(
        private val thermalRepository: ThermalRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<ThermalReading>> =
            proStatusProvider.isProUser
                .distinctUntilChanged()
                .flatMapLatest { isPro ->
                    if (!isPro) {
                        flowOf(emptyList())
                    } else {
                        val since =
                            when (period) {
                                HistoryPeriod.ALL, HistoryPeriod.SINCE_UNPLUG -> 0L
                                else -> System.currentTimeMillis() - period.durationMs
                            }
                        val limit = if (period == HistoryPeriod.ALL) 5_000 else null
                        thermalRepository.getReadingsSince(since, limit)
                    }
                }
    }
