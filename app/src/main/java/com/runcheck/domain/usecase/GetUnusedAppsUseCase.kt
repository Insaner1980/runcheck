package com.runcheck.domain.usecase

import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.domain.model.UnusedAppsResult
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UnusedAppsRepository
import java.time.Instant
import javax.inject.Inject

sealed interface UnusedAppsQueryResult {
    data object Locked : UnusedAppsQueryResult

    data class Available(
        val result: UnusedAppsResult,
    ) : UnusedAppsQueryResult
}

class GetUnusedAppsUseCase
    @Inject
    constructor(
        private val repository: UnusedAppsRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        suspend operator fun invoke(
            period: UnusedAppsPeriod,
            observedAt: Instant,
            forceRefresh: Boolean,
        ): UnusedAppsQueryResult {
            if (!proStatusProvider.isPro()) return UnusedAppsQueryResult.Locked
            return UnusedAppsQueryResult.Available(
                repository.getUnusedApps(period, observedAt, forceRefresh),
            )
        }
    }
