package com.runcheck.data.db.dao

import androidx.room.Room
import com.runcheck.data.db.RuncheckDatabase
import com.runcheck.data.db.entity.InsightEntity
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class InsightDaoSupportedRuleCleanupTest {
    private lateinit var database: RuncheckDatabase
    private lateinit var dao: InsightDao

    @Before
    fun setUp() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    RuncheckDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        dao = database.insightDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `unsupported persisted rule rows are deleted and supported rows remain`() =
        runTest {
            dao.insertAll(
                listOf(
                    insight(id = 1L, ruleId = "app_battery_impact"),
                    insight(id = 2L, ruleId = "heavy_app_usage"),
                ),
            )

            dao.deleteUnsupportedRuleIds(setOf("heavy_app_usage"))

            val remaining = dao.observeUndismissedInsights().first()
            assertEquals(listOf("heavy_app_usage"), remaining.map { it.ruleId })
        }

    private fun insight(
        id: Long,
        ruleId: String,
    ): InsightEntity =
        InsightEntity(
            id = id,
            ruleId = ruleId,
            dedupeKey = "dedupe-$id",
            type = InsightType.BATTERY.name,
            priority = InsightPriority.HIGH.sortOrder,
            confidence = 0.8f,
            titleKey = "title",
            bodyKey = "body",
            bodyArgsJson = "[]",
            generatedAt = 1_000L,
            expiresAt = 100_000L,
            dataWindowStart = 0L,
            dataWindowEnd = 1L,
            target = InsightTarget.BATTERY.name,
            dismissed = false,
            seen = false,
        )
}
