package com.runcheck.data.insights

import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.db.entity.InsightEntity
import com.runcheck.domain.insights.engine.InsightHomeRankingPolicy
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightCandidate
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.repository.DatabaseTransactionRunner
import com.runcheck.domain.repository.InsightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepositoryImpl
    @Inject
    constructor(
        private val insightDao: InsightDao,
        private val homeRankingPolicy: InsightHomeRankingPolicy,
        private val transactionRunner: DatabaseTransactionRunner,
    ) : InsightRepository {
        override fun getHomeInsights(limit: Int): Flow<List<Insight>> =
            observeActiveInsights()
                .map { insights -> homeRankingPolicy.selectHomeInsights(insights, limit) }
                .flowOn(Dispatchers.IO)

        override fun getActiveInsights(): Flow<List<Insight>> = observeActiveInsights().flowOn(Dispatchers.IO)

        override fun getUnseenCount(): Flow<Int> =
            insightDao
                .observeUndismissedInsights()
                .map { entities ->
                    val now = System.currentTimeMillis()
                    entities.count { !it.seen && it.expiresAt > now }
                }.flowOn(Dispatchers.IO)

        override suspend fun dismiss(id: Long) =
            withContext(Dispatchers.IO) {
                insightDao.dismiss(id)
            }

        override suspend fun markAllSeen() =
            withContext(Dispatchers.IO) {
                insightDao.markAllSeen()
            }

        override suspend fun clearAll() =
            withContext(Dispatchers.IO) {
                insightDao.deleteAll()
            }

        override suspend fun replaceRuleResults(
            ruleId: String,
            candidates: List<InsightCandidate>,
        ) {
            transactionRunner.runInTransaction {
                if (candidates.isEmpty()) {
                    insightDao.deleteByRule(ruleId)
                    return@runInTransaction
                }

                val existing = insightDao.getByRule(ruleId)
                val existingByDedupeKey = existing.associateBy { it.dedupeKey }
                val incomingKeys = candidates.map { it.dedupeKey }.toSet()
                val staleIds = existing.filter { it.dedupeKey !in incomingKeys }.map { it.id }

                if (staleIds.isNotEmpty()) {
                    insightDao.deleteByIds(staleIds)
                }

                val merged =
                    candidates.map { candidate ->
                        val existingEntry = existingByDedupeKey[candidate.dedupeKey]
                        candidate.toEntity(existingEntry)
                    }
                insightDao.insertAll(merged)
            }
        }

        override suspend fun deleteExpired(now: Long) =
            withContext(Dispatchers.IO) {
                insightDao.deleteExpired(now)
            }

        private fun observeActiveInsights(): Flow<List<Insight>> =
            insightDao
                .observeUndismissedInsights()
                .map { entities ->
                    val now = System.currentTimeMillis()
                    entities
                        .filter { it.expiresAt > now }
                        .map { it.toDomain() }
                }
    }

private fun InsightEntity.toDomain(): Insight =
    Insight(
        id = id,
        ruleId = ruleId,
        type = enumValueOf(type),
        priority = InsightPriority.entries.first { it.sortOrder == priority },
        confidence = confidence,
        titleKey = titleKey,
        bodyKey = bodyKey,
        bodyArgs = bodyArgsJson.toBodyArgs(),
        generatedAt = generatedAt,
        expiresAt = expiresAt,
        target = enumValueOf(target),
        seen = seen,
        dismissed = dismissed,
    )

private fun InsightCandidate.toEntity(existing: InsightEntity?): InsightEntity =
    InsightEntity(
        id = existing?.id ?: 0L,
        ruleId = ruleId,
        dedupeKey = dedupeKey,
        type = type.name,
        priority = priority.sortOrder,
        confidence = confidence,
        titleKey = titleKey,
        bodyKey = bodyKey,
        bodyArgsJson = bodyArgs.toJsonArrayString(),
        generatedAt = generatedAt,
        expiresAt = expiresAt,
        dataWindowStart = dataWindowStart,
        dataWindowEnd = dataWindowEnd,
        target = target.name,
        dismissed = existing?.dismissed ?: false,
        seen = existing?.seen ?: false,
    )

private fun List<String>.toJsonArrayString(): String = JSONArray(this).toString()

private fun String.toBodyArgs(): List<String> {
    val array = JSONArray(this)
    return List(array.length()) { index -> array.optString(index) }
}
