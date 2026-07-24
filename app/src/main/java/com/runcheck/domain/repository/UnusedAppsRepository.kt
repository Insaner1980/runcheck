package com.runcheck.domain.repository

import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.domain.model.UnusedAppsResult
import java.time.Instant

interface UnusedAppsRepository {
    suspend fun getUnusedApps(
        period: UnusedAppsPeriod,
        observedAt: Instant,
        forceRefresh: Boolean,
    ): UnusedAppsResult
}
