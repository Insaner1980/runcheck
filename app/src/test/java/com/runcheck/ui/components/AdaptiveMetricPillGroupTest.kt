package com.runcheck.ui.components

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class AdaptiveMetricPillGroupTest {
    @Test
    fun `normal text and scale immediately below threshold do not stack`() {
        assertFalse(shouldStackMetricPills(1f))
        assertFalse(shouldStackMetricPills(Math.nextDown(1.5f)))
    }

    @Test
    fun `threshold and scale immediately above it use stack`() {
        assertTrue(shouldStackMetricPills(1.5f))
        assertTrue(shouldStackMetricPills(Math.nextUp(1.5f)))
    }

    @Test
    fun `200 percent text uses stack`() {
        assertTrue(shouldStackMetricPills(2f))
    }

    @Test
    fun `both branches delegate ordered items confidence and info actions to existing renderer`() {
        val components = findRootDir().resolve("app/src/main/java/com/runcheck/ui/components")
        val group = components.resolve("AdaptiveMetricPillGroup.kt").readText()
        val renderer = components.resolve("MetricPill.kt").readText()
        val calls = Regex("MetricPillItems\\(\\s*items = items,\\s*onInfoClick = onInfoClick,").findAll(group)

        // Source contract only: no Compose rendering or accessibility interaction is exercised.
        assertEquals(2, calls.count())
        assertFalse(group.contains(".semantics"))
        assertTrue(renderer.contains("items.forEach { item ->"))
        assertTrue(renderer.contains("label = item.label"))
        assertTrue(renderer.contains("value = item.value"))
        assertTrue(renderer.contains("confidence = item.confidence"))
        assertTrue(renderer.contains("onInfoClick = item.infoKey?.let { key -> { onInfoClick(key) } }"))
    }
}
