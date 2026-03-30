package com.runcheck.debug.insights

import com.runcheck.data.db.dao.InsightDao
import com.runcheck.data.insights.debug.InsightTestDataSeeder
import com.runcheck.domain.insights.engine.InsightEngine
import com.runcheck.domain.repository.InsightDebugActions
import com.runcheck.domain.repository.InsightRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DebugInsightActionsImpl
    @Inject
    constructor(
        private val insightDao: InsightDao,
        private val insightRepository: InsightRepository,
        private val insightEngine: InsightEngine,
        private val insightTestDataSeeder: InsightTestDataSeeder,
    ) : InsightDebugActions {
        override val isAvailable: Boolean = true

        override suspend fun seedDemoInsights(): Int {
            val now = System.currentTimeMillis()
            insightTestDataSeeder.seed(now)
            insightEngine.generateInsights(now)
            return insightDao.countActive(now)
        }

        override suspend fun generateInsightsNow(): Int {
            val now = System.currentTimeMillis()
            insightEngine.generateInsights(now)
            return insightDao.countActive(now)
        }

        override suspend fun clearInsights(): Int {
            insightRepository.clearAll()
            return 0
        }
    }
