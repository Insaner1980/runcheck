package com.runcheck.ui.components

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.MainActivity
import com.runcheck.R
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ThemeMode
import com.runcheck.ui.theme.RuncheckTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignatureComponentRenderTest {
    @Test
    fun heroGauge_exposesOneLocalizedAccessibleDescriptionWithoutDescendantSpeech() {
        render {
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
            val accessibleText = accessibilityText()

            assertEquals(listOf(expectedDescription), accessibleText)
        }
    }

    @Test
    fun metricTile_hasEqualRenderedHeightForEveryStateAtNormalAndLargeFontScale() {
        listOf(1f, 2f).forEach { fontScale ->
            val heights =
                MetricTileState.entries.map { state ->
                    render {
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
                    }.use {
                        accessibilityNodeHeight("Battery health")
                    }
                }

            assertEquals("fontScale=$fontScale", 1, heights.distinct().size)
        }
    }

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit): RenderedComposeView {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        lateinit var composeView: ComposeView
        scenario.onActivity { activity ->
            composeView =
                ComposeView(activity).also { view ->
                    activity.setContentView(view)
                    view.setContent(content)
                }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        return RenderedComposeView(scenario)
    }

    private fun accessibilityText(): List<String> = accessibilityRoot().descendantText()

    private fun accessibilityNodeHeight(text: String): Int {
        val matchingNodes = accessibilityRoot().nodesContaining(text)
        assertFalse("Expected an accessibility node containing $text", matchingNodes.isEmpty())
        return matchingNodes.maxOf { node ->
            Rect().also(node::getBoundsInScreen).height()
        }
    }

    private fun accessibilityRoot(): AccessibilityNodeInfo {
        repeat(20) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation.rootInActiveWindow
                ?.let { return it }
            Thread.sleep(50)
        }
        throw AssertionError("The rendered app must expose an accessibility root")
    }

    private fun AccessibilityNodeInfo.descendantText(): List<String> {
        val text = mutableListOf<String>()

        fun visit(node: AccessibilityNodeInfo) {
            node.contentDescription?.toString()?.let(text::add)
            node.text?.toString()?.let(text::add)
            repeat(node.childCount) { childIndex ->
                node.getChild(childIndex)?.let(::visit)
            }
        }
        visit(this)
        return text
    }

    private fun AccessibilityNodeInfo.nodesContaining(value: String): List<AccessibilityNodeInfo> {
        val matches = mutableListOf<AccessibilityNodeInfo>()

        fun visit(node: AccessibilityNodeInfo) {
            if (node.contentDescription?.contains(value) == true || node.text?.contains(value) == true) {
                matches += node
            }
            repeat(node.childCount) { childIndex ->
                node.getChild(childIndex)?.let(::visit)
            }
        }
        visit(this)
        return matches
    }

    private class RenderedComposeView(
        private val scenario: ActivityScenario<MainActivity>,
    ) : AutoCloseable {
        override fun close() {
            scenario.close()
        }
    }
}
