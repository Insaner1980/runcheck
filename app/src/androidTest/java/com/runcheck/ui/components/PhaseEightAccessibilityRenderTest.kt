package com.runcheck.ui.components

import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.chart.HistoryPeriodSelectorRow
import com.runcheck.ui.theme.LocalReducedMotion
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class PhaseEightAccessibilityRenderTest {
    @Test
    fun reducedMotionLoadingSpinnerIsStaticAndKeepsIndeterminateSemantics() {
        renderCompose(widthPx = COMPACT_WIDTH, heightPx = SHORT_HEIGHT) {
            TestTheme(fontScale = 1f, reducedMotion = true) {
                RuncheckProgressSpinner(contentDescription = LOADING_DESCRIPTION)
            }
        }.use { rendered ->
            val loadingNode = rendered.nodeWithOwnTextContaining(LOADING_DESCRIPTION)
            val firstFrame = rendered.captureBitmap()
            Thread.sleep(STATIC_FRAME_DELAY_MILLIS)
            val secondFrame = rendered.captureBitmap()

            assertEquals("android.widget.ProgressBar", loadingNode.className?.toString())
            assertEquals(View.ACCESSIBILITY_LIVE_REGION_NONE, loadingNode.liveRegion)
            assertTrue("Reduced-motion spinner must not animate", firstFrame.sameAs(secondFrame))
        }
    }

    @Test
    fun largeFontSelectedHistoryOptionIsVisibleSelectedAndTouchable() {
        renderCompose(widthPx = COMPACT_WIDTH, heightPx = SHORT_HEIGHT) {
            TestTheme(fontScale = 2f, reducedMotion = true) {
                HistoryPeriodSelectorRow(
                    options = OPTIONS,
                    selected = OPTIONS.last(),
                    onSelect = {},
                    labelFor = { it },
                )
            }
        }.use { rendered ->
            val selectedNode =
                rendered
                    .nodesContainingText(OPTIONS.last())
                    .first {
                        it.isSelected ||
                            it.isChecked ||
                            it.stateDescription?.toString() == "Selected"
                    }
            val bounds = Rect().also(selectedNode::getBoundsInScreen)

            assertTrue(rendered.viewBounds().contains(bounds))
            assertTrue("Selected option must keep a 48dp touch target", bounds.height() >= MINIMUM_TOUCH_TARGET_PX)
        }
    }

    @Test
    fun headingsChartAndEmptyStateExposeOnlyMeaningfulSemantics() {
        var actionClicks = 0

        renderCompose(widthPx = COMPACT_WIDTH, heightPx = TALL_HEIGHT) {
            TestTheme(fontScale = 1.3f, reducedMotion = true) {
                Column {
                    SectionHeader(text = "History")
                    TrendChart(
                        data = listOf(20f, 28f, 25f, 34f),
                        chartHeight = 200.dp,
                        lineColor = Color.Blue,
                        fillColor = Color.Transparent,
                        contentDescription = CHART_DESCRIPTION,
                    )
                    StatusPill(label = "Healthy", tone = StatusTone.HEALTHY)
                    EmptyStateIllustration(
                        title = "No readings",
                        message = "Collect a reading to populate this chart.",
                        actionLabel = "Open details",
                        onAction = { actionClicks++ },
                    )
                }
            }
        }.use { rendered ->
            assertTrue(rendered.nodeWithOwnTextContaining("HISTORY").isHeading)
            val chartNode = rendered.nodeWithOwnTextContaining(CHART_DESCRIPTION)
            val chartBounds = Rect().also(chartNode::getBoundsInScreen)
            assertTrue(chartBounds.height() >= MINIMUM_CHART_HEIGHT_PX)
            assertTrue(rendered.accessibilityText().any { it.contains("Healthy") })
            assertFalse(rendered.accessibilityText().any { it.contains("Info") })

            val actionNode = rendered.clickableNodeContaining("Open details")
            val actionBounds = Rect().also(actionNode::getBoundsInScreen)
            assertTrue(actionBounds.height() >= MINIMUM_TOUCH_TARGET_PX)
            rendered.click("Open details")
        }

        assertEquals(1, actionClicks)
    }

    @Test
    fun metricTileRemainsClickableAcrossThemesAndSupportedFontScales() {
        ThemeMode.entries.forEach { themeMode ->
            listOf(1f, 1.3f, 2f).forEach { fontScale ->
                var clicks = 0
                renderCompose(widthPx = COMPACT_WIDTH, heightPx = TILE_HEIGHT) {
                    TestTheme(
                        fontScale = fontScale,
                        reducedMotion = true,
                        themeMode = themeMode,
                    ) {
                        MetricTile(
                            domain = MetricDomain.BATTERY,
                            value = "82",
                            unit = "%",
                            label = "Battery",
                            status = "Healthy",
                            confidence = Confidence.HIGH,
                            onClick = { clicks++ },
                        )
                    }
                }.use { rendered ->
                    val clickableNode = rendered.clickableNodeContaining("Battery")
                    val bounds = Rect().also(clickableNode::getBoundsInScreen)
                    assertTrue("$themeMode / $fontScale must stay in bounds", rendered.viewBounds().contains(bounds))
                    assertTrue(bounds.height() >= MINIMUM_TOUCH_TARGET_PX)
                    rendered.click("Battery")
                }
                assertEquals("$themeMode / $fontScale", 1, clicks)
            }
        }
    }

    @Composable
    private fun TestTheme(
        fontScale: Float,
        reducedMotion: Boolean,
        themeMode: ThemeMode = ThemeMode.DARK,
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = 1f, fontScale = fontScale),
        ) {
            RuncheckTheme(themeMode = themeMode) {
                CompositionLocalProvider(
                    LocalReducedMotion provides reducedMotion,
                    content = content,
                )
            }
        }
    }

    private companion object {
        const val COMPACT_WIDTH = 411
        const val SHORT_HEIGHT = 240
        const val TALL_HEIGHT = 900
        const val TILE_HEIGHT = 700
        const val MINIMUM_TOUCH_TARGET_PX = 48
        const val MINIMUM_CHART_HEIGHT_PX = 180
        const val STATIC_FRAME_DELAY_MILLIS = 400L
        const val LOADING_DESCRIPTION = "Loading health data"
        const val CHART_DESCRIPTION = "Battery level trend"
        val OPTIONS = listOf("Now", "Day", "Week", "Month", "Year")
    }
}
