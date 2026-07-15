package com.runcheck.data.insights

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import com.runcheck.util.AppDispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepositoryImpl
    @Inject
    constructor(
        private val insightDao: InsightDao,
        private val gson: Gson,
        private val homeRankingPolicy: InsightHomeRankingPolicy,
        private val transactionRunner: DatabaseTransactionRunner,
        private val dispatchers: AppDispatchers,
    ) : InsightRepository {
        override fun getHomeInsights(limit: Int): Flow<List<Insight>> =
            observeActiveInsights()
                .map { insights -> homeRankingPolicy.selectHomeInsights(insights, limit) }
                .flowOn(dispatchers.io)

        override fun getActiveInsights(): Flow<List<Insight>> = observeActiveInsights().flowOn(dispatchers.io)

        override fun getUnseenCount(): Flow<Int> =
            insightDao
                .observeUndismissedInsights()
                .map { entities ->
                    val now = System.currentTimeMillis()
                    entities.count { !it.seen && it.expiresAt > now }
                }.flowOn(dispatchers.io)

        override suspend fun dismiss(id: Long) = insightDao.dismiss(id)

        override suspend fun markAllSeen() = insightDao.markAllSeen()

        override suspend fun clearAll() = insightDao.deleteAll()

        override suspend fun replaceGenerationResults(
            candidatesByRule: Map<String, List<InsightCandidate>>,
            now: Long,
        ) {
            transactionRunner.runInTransaction {
                insightDao.deleteExpired(now)
                candidatesByRule.forEach { (ruleId, candidates) ->
                    replaceRuleResults(ruleId, candidates)
                }
            }
        }

        private suspend fun replaceRuleResults(
            ruleId: String,
            candidates: List<InsightCandidate>,
        ) {
            if (candidates.isEmpty()) {
                insightDao.deleteByRule(ruleId)
                return
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
                    candidate.toEntity(existingEntry, gson)
                }
            insightDao.insertAll(merged)
        }

        private fun observeActiveInsights(): Flow<List<Insight>> =
            insightDao
                .observeUndismissedInsights()
                .map { entities ->
                    val now = System.currentTimeMillis()
                    entities
                        .filter { it.expiresAt > now }
                        .map { it.toDomain(gson) }
                }
    }

private fun InsightEntity.toDomain(gson: Gson): Insight =
    Insight(
        id = id,
        ruleId = ruleId,
        type = enumValueOf(type),
        priority = InsightPriority.entries.first { it.sortOrder == priority },
        confidence = confidence,
        titleKey = titleKey,
        bodyKey = bodyKey,
        bodyArgs = bodyArgsJson.toBodyArgs(gson),
        generatedAt = generatedAt,
        expiresAt = expiresAt,
        target = enumValueOf(target),
        seen = seen,
        dismissed = dismissed,
    )

private fun InsightCandidate.toEntity(
    existing: InsightEntity?,
    gson: Gson,
): InsightEntity =
    InsightEntity(
        id = existing?.id ?: 0L,
        ruleId = ruleId,
        dedupeKey = dedupeKey,
        type = type.name,
        priority = priority.sortOrder,
        confidence = confidence,
        titleKey = titleKey,
        bodyKey = bodyKey,
        bodyArgsJson = bodyArgs.toJsonArrayString(gson),
        generatedAt = generatedAt,
        expiresAt = expiresAt,
        dataWindowStart = dataWindowStart,
        dataWindowEnd = dataWindowEnd,
        target = target.name,
        dismissed = existing?.dismissed ?: false,
        seen = existing?.seen ?: false,
    )

private val stringListType = object : TypeToken<List<String>>() {}.type

private fun List<String>.toJsonArrayString(gson: Gson): String = gson.toJson(this, stringListType)

private fun String.toBodyArgs(gson: Gson): List<String> =
    gson.fromJson<List<String>>(this, stringListType) ?: emptyList()
