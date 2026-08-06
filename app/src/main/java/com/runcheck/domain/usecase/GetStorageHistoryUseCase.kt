package com.runcheck.domain.usecase

import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageHistoryUseCase
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(period: HistoryPeriod = HistoryPeriod.WEEK): Flow<List<StorageReading>> =
            proStatusProvider.proHistoryFlow(period, storageRepository::getReadingsSince)
    }
