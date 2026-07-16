package com.runcheck.domain.repository

import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightCandidate
import kotlinx.coroutines.flow.Flow

interface InsightRepository {
    fun getActiveInsights(): Flow<List<Insight>>

    fun getUnseenCount(): Flow<Int>

    suspend fun dismiss(id: Long)

    suspend fun markSeen(ids: Set<Long>)

    suspend fun clearAll()

    suspend fun replaceGenerationResults(
        candidatesByRule: Map<String, List<InsightCandidate>>,
        now: Long,
    )
}
