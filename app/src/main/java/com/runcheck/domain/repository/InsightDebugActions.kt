package com.runcheck.domain.repository

interface InsightDebugActions {
    val isAvailable: Boolean

    suspend fun seedDemoInsights(): Int

    suspend fun generateInsightsNow(): Int

    suspend fun clearInsights(): Int
}
