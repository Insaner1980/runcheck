package com.runcheck.domain.repository

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightCandidate
import kotlinx.coroutines.flow.Flow

interface InsightRepository {
    fun getHomeInsights(limit: Int = 10): Flow<List<Insight>>

    fun getActiveInsights(): Flow<List<Insight>>

    fun getUnseenCount(): Flow<Int>

    suspend fun dismiss(id: Long)

    suspend fun markAllSeen()

    suspend fun clearAll()

    suspend fun replaceRuleResults(
        ruleId: String,
        candidates: List<InsightCandidate>,
    )

    suspend fun deleteExpired(now: Long)
}
