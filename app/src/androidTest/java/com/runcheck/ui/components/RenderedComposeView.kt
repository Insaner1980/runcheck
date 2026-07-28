package com.runcheck.ui.components

import android.graphics.Rect
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

internal fun renderCompose(
    widthPx: Int? = null,
    heightPx: Int? = null,
    content: @Composable () -> Unit,
): RenderedComposeView {
    require((widthPx == null) == (heightPx == null))
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    lateinit var composeView: ComposeView
    scenario.onActivity { activity ->
        composeView = ComposeView(activity)
        if (widthPx != null && heightPx != null) {
            activity.setContentView(
                composeView,
                ViewGroup.LayoutParams(widthPx, heightPx),
            )
        } else {
            activity.setContentView(composeView)
        }
        composeView.setContent(content)
    }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    return RenderedComposeView(
        scenario = scenario,
        composeView = composeView,
    )
}

internal class RenderedComposeView(
    private val scenario: ActivityScenario<MainActivity>,
    private val composeView: ComposeView,
) : AutoCloseable {
    fun accessibilityText(): List<String> = accessibilityRoot().descendantText()

    fun accessibilityNodeHeight(text: String): Int {
        val matchingNodes = nodesContaining(text)
        assertFalse("Expected an accessibility node containing $text", matchingNodes.isEmpty())
        return matchingNodes.maxOf { node ->
            Rect().also(node::getBoundsInScreen).height()
        }
    }

    fun nodeContaining(text: String): AccessibilityNodeInfo {
        val matchingNodes = nodesContaining(text)
        assertFalse("Expected an accessibility node containing $text", matchingNodes.isEmpty())
        return matchingNodes.first()
    }

    fun clickableNodeContaining(text: String): AccessibilityNodeInfo {
        val matchingNode =
            accessibilityRoot()
                .allNodes()
                .firstOrNull { node ->
                    node.isClickable && node.descendantText().any { it.contains(text, ignoreCase = false) }
                }
        assertTrue("Expected a clickable accessibility node containing $text", matchingNode != null)
        return checkNotNull(matchingNode)
    }

    fun click(text: String) {
        val clicked =
            clickableNodeContaining(text)
                .performAction(AccessibilityNodeInfo.ACTION_CLICK)
        assertTrue("Expected click action to succeed for $text", clicked)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    fun viewBounds(): Rect {
        val location = IntArray(2)
        composeView.getLocationOnScreen(location)
        return Rect(
            location[0],
            location[1],
            location[0] + composeView.width,
            location[1] + composeView.height,
        )
    }

    fun nodesContaining(value: String): List<AccessibilityNodeInfo> =
        accessibilityRoot()
            .allNodes()
            .filter { node ->
                node.descendantText().any { it.contains(value, ignoreCase = false) }
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

    override fun close() {
        scenario.close()
    }
}

private fun AccessibilityNodeInfo.allNodes(): List<AccessibilityNodeInfo> {
    val nodes = mutableListOf<AccessibilityNodeInfo>()

    fun visit(node: AccessibilityNodeInfo) {
        nodes += node
        repeat(node.childCount) { childIndex ->
            node.getChild(childIndex)?.let(::visit)
        }
    }
    visit(this)
    return nodes
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
