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
        val node = nodeWithOwnTextContaining(text)
        return Rect().also(node::getBoundsInScreen).height()
    }

    fun nodeWithOwnTextContaining(text: String): AccessibilityNodeInfo {
        val matchingNodes = nodesWithOwnTextContaining(text)
        assertFalse("Expected an accessibility node with its own text containing $text", matchingNodes.isEmpty())
        return matchingNodes.first()
    }

    fun nodesWithOwnTextContaining(value: String): List<AccessibilityNodeInfo> =
        accessibilityRoot()
            .allNodes()
            .filter { node ->
                node.ownText().any { it.contains(value, ignoreCase = false) }
            }

    private fun nodesContainingDescendantText(value: String): List<AccessibilityNodeInfo> =
        accessibilityRoot()
            .allNodes()
            .filter { node ->
                node.descendantText().any { it.contains(value, ignoreCase = false) }
            }

    fun clickableNodeContaining(text: String): AccessibilityNodeInfo {
        val matchingNode =
            nodesContainingDescendantText(text)
                .firstOrNull { node -> node.isClickable }
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

private fun AccessibilityNodeInfo.ownText(): List<String> =
    buildList {
        contentDescription?.toString()?.let(::add)
        text?.toString()?.let(::add)
    }

private fun AccessibilityNodeInfo.descendantText(): List<String> {
    val text = mutableListOf<String>()

    fun visit(node: AccessibilityNodeInfo) {
        text += node.ownText()
        repeat(node.childCount) { childIndex ->
            node.getChild(childIndex)?.let(::visit)
        }
    }
    visit(this)
    return text
}
