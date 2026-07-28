package com.runcheck.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.R
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignatureComponentRenderTest {
    @Test
    fun heroGauge_exposesOneLocalizedAccessibleDescriptionWithoutDescendantSpeech() {
        renderCompose {
            RuncheckTheme(themeMode = ThemeMode.DARK) {
                HeroGauge(
                    value = 81.6f,
                    label = "Battery",
                    status = "Good",
                    accent = Color.Green,
                    contentDescription = "Battery health",
                    animationKey = "hero-gauge-render-test",
                    confidence = Confidence.HIGH,
                )
            }
        }.use { rendered ->
            val expectedDescription =
                InstrumentationRegistry.getInstrumentation().targetContext.getString(
                    R.string.hero_gauge_semantics_with_confidence,
                    "Battery health",
                    82,
                    "Good",
                    "Accurate",
                )

            assertEquals(listOf(expectedDescription), rendered.accessibilityText())
        }
    }

    @Test
    fun metricTile_hasEqualRenderedHeightForEveryStateAtNormalAndLargeFontScale() {
        listOf(1f, 2f).forEach { fontScale ->
            val heights =
                MetricTileState.entries.map { state ->
                    renderCompose {
                        CompositionLocalProvider(
                            LocalDensity provides Density(density = 1f, fontScale = fontScale),
                        ) {
                            RuncheckTheme(themeMode = ThemeMode.DARK) {
                                MetricTile(
                                    domain = MetricDomain.BATTERY,
                                    value = "82",
                                    unit = "%",
                                    label = "Battery health",
                                    state = state,
                                    status = if (state == MetricTileState.READY) "Good" else null,
                                    confidence = if (state == MetricTileState.READY) Confidence.HIGH else null,
                                )
                            }
                        }
                    }.use { rendered ->
                        val metricTileHeight = rendered.accessibilityNodeHeight("Battery health")

                        assertTrue(
                            "The measured node must be the MetricTile semantics node, not the accessibility root",
                            metricTileHeight < rendered.viewBounds().height(),
                        )
                        metricTileHeight
                    }
                }

            assertEquals("fontScale=$fontScale", 1, heights.distinct().size)
        }
    }
}
