package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.ThermalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetThermalHistoryUseCase
    @Inject
    constructor(
        private val thermalRepository: ThermalRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.DAY): Flow<List<ThermalReading>> =
            proStatusProvider.proHistoryFlow(period, thermalRepository::getReadingsSince)
    }
