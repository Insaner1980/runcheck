package com.runcheck.domain.insights.rules

import com.runcheck.domain.insights.analysis.BatteryDrainAnalyzer
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.BatteryReading
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineAnomalyRuleTest {
    @Test
    fun `emits battery insight when recent drain is well above historical baseline`() =
        runTest {
            val now = DAY_MS * 20
            val readings =
                baselineDrainDays(now, dropPerHour = 1, dayCount = 14) +
                    recentDrainDay(now, dropPerHour = 5)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            val result = rule.evaluate(now)

            assertEquals(1, result.size)
            val insight = result.single()
            assertEquals(BaselineAnomalyRule.RULE_ID, insight.ruleId)
            assertEquals("battery_drain:3xplus", insight.dedupeKey)
            assertEquals(InsightType.BATTERY, insight.type)
            assertEquals(InsightPriority.HIGH, insight.priority)
            assertEquals(InsightTarget.BATTERY, insight.target)
            assertEquals(listOf("5", "1"), insight.bodyArgs)
            assertTrue(insight.confidence >= 0.7f)
        }

    @Test
    fun `returns empty when baseline history has too few daily windows`() =
        runTest {
            val now = DAY_MS * 20
            val readings =
                baselineDrainDays(now, dropPerHour = 1, dayCount = 6) +
                    recentDrainDay(now, dropPerHour = 5)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `emits medium priority insight when drain ratio is anomalous but below high threshold`() =
        runTest {
            val now = DAY_MS * 20
            val baselineDrops = List(14) { day -> if (day % 2 == 0) 1 else 3 }
            val readings =
                baselineDrainDays(now, baselineDrops) +
                    recentDrainDay(now, dropPerHour = 5)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            val insight = rule.evaluate(now).single()

            assertEquals("battery_drain:25xplus", insight.dedupeKey)
            assertEquals(InsightPriority.MEDIUM, insight.priority)
            assertEquals(listOf("5", "2"), insight.bodyArgs)
        }

    @Test
    fun `returns empty when recent drain is close to baseline`() =
        runTest {
            val now = DAY_MS * 20
            val readings =
                baselineDrainDays(now, dropPerHour = 2, dayCount = 14) +
                    recentDrainDay(now, dropPerHour = 3)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns empty when sample deviation keeps noisy baseline below anomaly threshold`() =
        runTest {
            val now = DAY_MS * 20
            val readings =
                baselineDrainDays(now, listOf(1, 1, 1, 1, 1, 3, 3)) +
                    recentDrainDay(now, dropPerHour = 4)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    @Test
    fun `returns empty when current window has too few fully discharging pairs`() =
        runTest {
            val now = DAY_MS * 20
            val readings =
                baselineDrainDays(now, dropPerHour = 1, dayCount = 14) +
                    interruptedDrainWindow(start = now - DAY_MS, dropPerHour = 5)
            val rule =
                BaselineAnomalyRule(
                    batteryRepository = TestBatteryRepository(readings),
                    batteryDrainAnalyzer = BatteryDrainAnalyzer(),
                )

            assertTrue(rule.evaluate(now).isEmpty())
        }

    private fun baselineDrainDays(
        now: Long,
        dropPerHourByDay: List<Int>,
    ): List<BatteryReading> {
        val baselineStart = now - DAY_MS - (dropPerHourByDay.size * DAY_MS)
        return dropPerHourByDay.flatMapIndexed { day, dropPerHour ->
            drainWindow(
                start = baselineStart + (day * DAY_MS),
                dropPerHour = dropPerHour,
            )
        }
    }

    private fun baselineDrainDays(
        now: Long,
        dropPerHour: Int,
        dayCount: Int,
    ): List<BatteryReading> {
        val baselineStart = now - DAY_MS - (dayCount * DAY_MS)
        return (0 until dayCount).flatMap { day ->
            drainWindow(
                start = baselineStart + (day * DAY_MS),
                dropPerHour = dropPerHour,
            )
        }
    }

    private fun recentDrainDay(
        now: Long,
        dropPerHour: Int,
    ): List<BatteryReading> =
        drainWindow(
            start = now - DAY_MS,
            dropPerHour = dropPerHour,
        )

    private fun drainWindow(
        start: Long,
        dropPerHour: Int,
    ): List<BatteryReading> =
        (0..5).map { index ->
            batteryReading(
                timestamp = start + (index * HOUR_MS),
                level = 100 - (index * dropPerHour),
            )
        }

    private fun interruptedDrainWindow(
        start: Long,
        dropPerHour: Int,
    ): List<BatteryReading> {
        val statuses =
            listOf(
                "CHARGING",
                "DISCHARGING",
                "DISCHARGING",
                "CHARGING",
                "DISCHARGING",
                "CHARGING",
                "DISCHARGING",
                "CHARGING",
                "DISCHARGING",
            )
        return statuses.mapIndexed { index, status ->
            batteryReading(
                timestamp = start + (index * HOUR_MS),
                level = 100 - (index * dropPerHour),
            ).copy(status = status)
        }
    }

    private companion object {
        private const val HOUR_MS = 60L * 60L * 1000L
        private const val DAY_MS = 24L * HOUR_MS
    }
}
