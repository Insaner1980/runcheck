package com.runcheck.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.runcheck.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.io.File
import java.io.FileOutputStream

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

    fun waitUntilTextAbsent(
        text: String,
        timeoutMillis: Long = 2_000L,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (SystemClock.uptimeMillis() < deadline) {
            if (accessibilityText().none { it.contains(text, ignoreCase = false) }) return
            Thread.sleep(50L)
        }
        assertFalse(
            "Expected accessibility text containing $text to disappear",
            accessibilityText().any { it.contains(text, ignoreCase = false) },
        )
    }

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

    fun nodesContainingText(value: String): List<AccessibilityNodeInfo> = nodesContainingDescendantText(value)

    fun activateOwnText(text: String) {
        val matchingNodes = nodesWithOwnTextContaining(text)
        assertFalse("Expected an accessibility node with its own text containing $text", matchingNodes.isEmpty())
        val textNode =
            matchingNodes
                .filter { node -> node.boundsArea() > 0L }
                .minBy(AccessibilityNodeInfo::boundsArea)
        val textBounds = Rect().also(textNode::getBoundsInScreen)
        val clickTarget =
            accessibilityRoot()
                .allNodes()
                .filter(AccessibilityNodeInfo::isClickable)
                .filter { node ->
                    Rect().also(node::getBoundsInScreen).contains(
                        textBounds.centerX(),
                        textBounds.centerY(),
                    )
                }.minByOrNull(AccessibilityNodeInfo::boundsArea)
        assertTrue("Expected a clickable node at the own-text center for $text", clickTarget != null)
        val clicked = checkNotNull(clickTarget).performAction(AccessibilityNodeInfo.ACTION_CLICK)
        assertTrue("Expected accessibility click action to succeed for $text", clicked)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
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

    fun captureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(composeView.width, composeView.height, Bitmap.Config.ARGB_8888)
        scenario.onActivity {
            composeView.draw(Canvas(bitmap))
        }
        return bitmap
    }

    fun capturePng(fileName: String): File {
        require(fileName.endsWith(".png"))
        require('/' !in fileName && '\\' !in fileName)
        val outputDirectory =
            File(
                InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
                "phase-eight-evidence",
            ).apply { mkdirs() }
        val outputFile = File(outputDirectory, fileName)
        FileOutputStream(outputFile).use { output ->
            check(captureBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
            "mkdir -p $SHARED_EVIDENCE_DIRECTORY",
            "cp ${outputFile.absolutePath} $SHARED_EVIDENCE_DIRECTORY/$fileName",
        ).forEach { command ->
            ParcelFileDescriptor.AutoCloseInputStream(uiAutomation.executeShellCommand(command)).use { output ->
                output.readBytes()
            }
        }
        return outputFile
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

private const val SHARED_EVIDENCE_DIRECTORY = "/sdcard/Download/runcheck-phase-eight"

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

private fun AccessibilityNodeInfo.boundsArea(): Long {
    val bounds = Rect()
    getBoundsInScreen(bounds)
    return bounds.width().toLong() * bounds.height()
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
