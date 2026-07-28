package com.runcheck.ui.home

import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.R
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThemeMode
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.ui.components.renderCompose
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightsCard
import com.runcheck.ui.home.insights.InsightsCardState
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.tools.ToolsScreen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeToolsRenderTest {
    @Test
    fun homeHero_largeFontGaugeFitsAndKeepsItsCombinedSemantics() {
        renderCompose(
            widthPx = COMPACT_WIDTH,
            heightPx = HERO_TEST_HEIGHT,
        ) {
            LargeFontTheme {
                HomeHealthHero(
                    healthScore = homeState.healthScore,
                    measurementTimestampMillis = homeState.measurementTimestampMillis,
                    measurementConfidence = homeState.batteryState.currentMa.confidence,
                )
            }
        }.use { rendered ->
            val expectedDescription =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(
                    R.string.hero_gauge_semantics_with_confidence,
                    InstrumentationRegistry.getInstrumentation().targetContext.getString(
                        R.string.a11y_health_score,
                        homeState.healthScore.overallScore,
                    ),
                    homeState.healthScore.overallScore,
                    "Healthy",
                    "Accurate",
                )
            val gaugeNode =
                rendered
                    .nodesWithOwnTextContaining(expectedDescription)
                    .first { it.contentDescription?.toString() == expectedDescription }
            val gaugeBounds = Rect().also(gaugeNode::getBoundsInScreen)

            assertTrue(rendered.viewBounds().contains(gaugeBounds))
            assertTrue(gaugeBounds.width() >= LARGE_GAUGE_MINIMUM_PX)
            assertTrue(gaugeBounds.height() >= LARGE_GAUGE_MINIMUM_PX)
        }
    }

    @Test
    fun homeMetricTilesInvokeEveryDomainNavigationCallback() {
        var batteryClicks = 0
        var networkClicks = 0
        var thermalClicks = 0
        var storageClicks = 0

        renderCompose(
            widthPx = COMPACT_WIDTH,
            heightPx = HOME_GRID_TEST_HEIGHT,
        ) {
            NormalFontTheme {
                HomeGridSection(
                    state = homeState,
                    isWideScreen = false,
                    onNavigateToBattery = { batteryClicks++ },
                    onNavigateToNetwork = { networkClicks++ },
                    onNavigateToThermal = { thermalClicks++ },
                    onNavigateToStorage = { storageClicks++ },
                )
            }
        }.use { rendered ->
            rendered.click("Battery")
            rendered.click("Network")
            rendered.click("Thermal")
            rendered.click("Storage")
        }

        assertEquals(1, batteryClicks)
        assertEquals(1, networkClicks)
        assertEquals(1, thermalClicks)
        assertEquals(1, storageClicks)
    }

    @Test
    fun toolsHierarchyInvokesEveryExistingNavigationCallback() {
        val clickCounts = IntArray(6)

        renderCompose(
            widthPx = COMPACT_WIDTH,
            heightPx = TOOLS_TEST_HEIGHT,
        ) {
            NormalFontTheme {
                ToolsScreen(
                    onNavigateToSpeedTest = { clickCounts[0]++ },
                    onNavigateToStorageCleanup = { clickCounts[1]++ },
                    onNavigateToCharger = { clickCounts[2]++ },
                    onNavigateToAppUsage = { clickCounts[3]++ },
                    onNavigateToLearn = { clickCounts[4]++ },
                    onNavigateToExport = { clickCounts[5]++ },
                    hasProAccess = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }.use { rendered ->
            rendered.click("Run speed test")
            rendered.click("Review storage")
            rendered.click("Charger Comparison")
            rendered.click("App Usage")
            rendered.click("Learn")
            rendered.click("Export")
        }

        clickCounts.forEachIndexed { index, clickCount ->
            assertEquals("callback index=$index", 1, clickCount)
        }
    }

    @Test
    fun emptyHomeInsightsShowsUsefulContentHeadingAndClickableCta() {
        var viewAllClicks = 0
        val noOp = {}

        renderCompose(
            widthPx = COMPACT_WIDTH,
            heightPx = INSIGHTS_TEST_HEIGHT,
        ) {
            NormalFontTheme {
                InsightsCard(
                    state =
                        InsightsCardState(
                            insights = emptyList(),
                            unseenInsightCount = 0,
                            isPro = false,
                        ),
                    navigationHandlers =
                        InsightNavigationHandlers(
                            onNavigateToBattery = noOp,
                            onNavigateToNetwork = noOp,
                            onNavigateToThermal = noOp,
                            onNavigateToStorage = noOp,
                            onNavigateToCharger = noOp,
                            onNavigateToAppUsage = noOp,
                            onNavigateToProUpgrade = noOp,
                        ),
                    onNavigateToInsights = { viewAllClicks++ },
                    onDismissInsight = {},
                )
            }
        }.use { rendered ->
            val text = rendered.accessibilityText()

            assertTrue(text.any { it.contains("No active insights") })
            assertTrue(text.any { it.contains("Open Insights to review") })
            assertTrue(rendered.nodesWithOwnTextContaining("INSIGHTS").any { it.isHeading })
            rendered.click("View all")
        }

        assertEquals(1, viewAllClicks)
    }

    private val homeState: HomeUiState.Success by lazy {
        val battery =
            BatteryState(
                level = 85,
                voltageMv = 4_100,
                temperatureC = 28f,
                currentMa = MeasuredValue(value = -350, confidence = Confidence.HIGH),
                chargingStatus = ChargingStatus.DISCHARGING,
                plugType = PlugType.NONE,
                health = BatteryHealth.GOOD,
                technology = "Li-ion",
            )
        val network =
            NetworkState(
                connectionType = ConnectionType.WIFI,
                signalDbm = -55,
                signalQuality = SignalQuality.EXCELLENT,
                wifiSsid = "TestWiFi",
            )
        val thermal =
            ThermalState(
                batteryTempC = 28f,
                cpuTempC = 45f,
                thermalStatus = ThermalStatus.NONE,
                isThrottling = false,
            )
        val storage =
            StorageState(
                totalBytes = 128_000_000_000L,
                availableBytes = 64_000_000_000L,
                usedBytes = 64_000_000_000L,
                usagePercent = 50f,
            )

        HomeUiState.Success(
            healthScore =
                HealthScoreCalculator().calculate(
                    battery = battery,
                    network = network,
                    thermal = thermal,
                    storage = storage,
                ),
            batteryState = battery,
            networkState = network,
            thermalState = thermal,
            storageState = storage,
            measurementTimestampMillis = 1_700_000_000_000L,
        )
    }

    @androidx.compose.runtime.Composable
    private fun NormalFontTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 1f, fontScale = 1f),
        ) {
            RuncheckTheme(themeMode = ThemeMode.DARK, content = content)
        }
    }

    @androidx.compose.runtime.Composable
    private fun LargeFontTheme(content: @androidx.compose.runtime.Composable () -> Unit) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 1f, fontScale = 2f),
        ) {
            RuncheckTheme(themeMode = ThemeMode.DARK, content = content)
        }
    }

    private companion object {
        const val COMPACT_WIDTH = 411
        const val HERO_TEST_HEIGHT = 600
        const val HOME_GRID_TEST_HEIGHT = 700
        const val TOOLS_TEST_HEIGHT = 1_600
        const val INSIGHTS_TEST_HEIGHT = 500
        const val LARGE_GAUGE_MINIMUM_PX = 140
    }
}
