package com.runcheck.ui.insights

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightPriority
import com.runcheck.domain.insights.model.InsightTarget
import com.runcheck.domain.insights.model.InsightType
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.WeeklyBatterySummary
import com.runcheck.domain.model.WeeklyReport
import com.runcheck.domain.model.WeeklyReportAvailability
import com.runcheck.domain.model.WeeklyReportCoverage
import com.runcheck.domain.model.WeeklyReportPeriod
import com.runcheck.domain.model.WeeklySpeedSummary
import com.runcheck.domain.model.WeeklyStorageSummary
import com.runcheck.domain.model.WeeklyThermalSummary
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.weekly.WeeklyReportUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class PhaseSevenRenderTest {
    @Test
    fun emptyInsightsOffersUsefulHomeAndWeeklyReportActions() {
        var homeClicks = 0
        var weeklyReportClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                InsightsContent(
                    state = InsightsUiState.Success(isPro = false),
                    weeklyReportState = WeeklyReportUiState.Locked,
                    navigationHandlers = navigationHandlers,
                    onDismissInsight = {},
                    onNavigateHome = { homeClicks++ },
                    onNavigateToWeeklyReport = { weeklyReportClicks++ },
                    selectedFilter = InsightFilter.ALL,
                    onSelectFilter = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { rendered ->
            val text = rendered.accessibilityText()
            assertTrue(text.any { it.contains("No active insights right now") })
            assertTrue(text.any { it.contains("THIS WEEK") })
            rendered.click("Go to Home")
            rendered.click("Open Weekly Report")
        }

        assertEquals(1, homeClicks)
        assertEquals(1, weeklyReportClicks)
    }

    @Test
    fun insightsGroupsAttentionItemsAndShowsCurrentWeeklySummary() {
        val high = insight(id = 1L, title = "High priority", priority = InsightPriority.HIGH)
        val low = insight(id = 2L, title = "Low priority", priority = InsightPriority.LOW)

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                InsightsContent(
                    state = InsightsUiState.Success(insights = listOf(low, high), isPro = true),
                    weeklyReportState = WeeklyReportUiState.Success(weeklyReport),
                    navigationHandlers = navigationHandlers,
                    onDismissInsight = {},
                    onNavigateHome = {},
                    onNavigateToWeeklyReport = {},
                    selectedFilter = InsightFilter.ALL,
                    onSelectFilter = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { rendered ->
            val text = rendered.accessibilityText()
            assertTrue(text.any { it.contains("NEEDS ATTENTION") })
            assertTrue(text.any { it.contains("OTHER INSIGHTS") })
            assertTrue(text.any { it.contains("6") })
            assertTrue(text.any { it.contains("84") })
            assertTrue(text.any { it.contains("3") })
        }
    }

    @Test
    fun importantFilterRendersSelectedSemanticsAndHidesLowPriorityItems() {
        val high = insight(id = 1L, title = "High priority", priority = InsightPriority.HIGH)
        val low = insight(id = 2L, title = "Low priority", priority = InsightPriority.LOW)

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                InsightsContent(
                    state = InsightsUiState.Success(insights = listOf(low, high), isPro = false),
                    weeklyReportState = WeeklyReportUiState.Locked,
                    navigationHandlers = navigationHandlers,
                    onDismissInsight = {},
                    onNavigateHome = {},
                    onNavigateToWeeklyReport = {},
                    selectedFilter = InsightFilter.IMPORTANT,
                    onSelectFilter = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { rendered ->
            val textAfterSelection = rendered.accessibilityText()
            assertTrue(textAfterSelection.any { it.contains("High priority") })
            assertFalse(textAfterSelection.any { it.contains("Low priority") })
            val importantNodes = rendered.nodesContainingText("Important")
            assertTrue(
                "Important semantics: " +
                    importantNodes.joinToString { node ->
                        "class=${node.className}, selected=${node.isSelected}, " +
                            "state=${node.stateDescription}, text=${node.text}"
                    },
                importantNodes.any { node ->
                    node.isSelected ||
                        node.stateDescription?.toString() == "Selected"
                },
            )
        }
    }

    private fun insight(
        id: Long,
        title: String,
        priority: InsightPriority,
    ) = Insight(
        id = id,
        ruleId = "rule-$id",
        type = InsightType.BATTERY,
        priority = priority,
        confidence = 0.9f,
        titleKey = title,
        bodyKey = "Body",
        bodyArgs = emptyList(),
        generatedAt = 0L,
        expiresAt = Long.MAX_VALUE,
        target = InsightTarget.BATTERY,
        seen = false,
        dismissed = false,
    )

    private val navigationHandlers =
        InsightNavigationHandlers(
            onNavigateToBattery = {},
            onNavigateToNetwork = {},
            onNavigateToThermal = {},
            onNavigateToStorage = {},
            onNavigateToCharger = {},
            onNavigateToAppUsage = {},
            onNavigateToProUpgrade = {},
        )

    private val weeklyReport =
        WeeklyReport(
            period =
                WeeklyReportPeriod(
                    startInclusive = Instant.parse("2026-07-13T00:00:00Z"),
                    endExclusive = Instant.parse("2026-07-20T00:00:00Z"),
                    zoneId = ZoneId.of("UTC"),
                ),
            coverage =
                WeeklyReportCoverage(
                    monitoredDays = 6,
                    sampleCount = 84,
                    availability = WeeklyReportAvailability.ESTIMATED,
                ),
            battery =
                WeeklyBatterySummary(
                    averageDischargePercentPerHour = null,
                    dischargePercentChange = 0.0,
                    chargePercentChange = 0.0,
                    healthPercentChange = null,
                    availability = WeeklyReportAvailability.UNAVAILABLE,
                    validSegmentCount = 0,
                ),
            storage =
                WeeklyStorageSummary(
                    availableBytesChange = null,
                    availability = WeeklyReportAvailability.UNAVAILABLE,
                ),
            thermal =
                WeeklyThermalSummary(
                    throttlingEventCount = 0,
                    highestThermalStatus = null,
                    availability = WeeklyReportAvailability.UNAVAILABLE,
                ),
            speed =
                WeeklySpeedSummary(
                    testCount = 3,
                    medianDownloadMbps = 100.0,
                    medianUploadMbps = 20.0,
                    medianLatencyMs = 15.0,
                    availability = WeeklyReportAvailability.AVAILABLE,
                ),
            topApps = emptyList(),
            availability = WeeklyReportAvailability.ESTIMATED,
        )

    private companion object {
        const val COMPACT_WIDTH = 411
        const val TALL_HEIGHT = 1_600
    }
}
