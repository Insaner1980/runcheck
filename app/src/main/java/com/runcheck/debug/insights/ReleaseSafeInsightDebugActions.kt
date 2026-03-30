package com.runcheck.debug.insights

import com.runcheck.domain.repository.InsightDebugActions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseSafeInsightDebugActions
    @Inject
    constructor() : InsightDebugActions {
        override val isAvailable: Boolean = false

        override suspend fun seedDemoInsights(): Int = 0

        override suspend fun generateInsightsNow(): Int = 0

        override suspend fun clearInsights(): Int = 0
    }
