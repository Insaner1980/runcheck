package com.runcheck.domain.usecase

import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import javax.inject.Inject

class SetDataRetentionUseCase
    @Inject
    constructor(
        private val userPreferencesRepository: UserPreferencesRepository,
        private val proStatusProvider: ProStatusProvider,
        private val cleanupOldReadingsUseCase: CleanupOldReadingsUseCase,
    ) {
        suspend operator fun invoke(retention: DataRetention) {
            if (!proStatusProvider.isPro()) return
            userPreferencesRepository.setDataRetention(retention)
            cleanupOldReadingsUseCase()
        }
    }
