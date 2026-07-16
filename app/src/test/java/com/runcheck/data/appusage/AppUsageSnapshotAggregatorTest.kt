package com.runcheck.data.appusage

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUsageSnapshotAggregatorTest {
    @Test
    fun `adjacent collection windows count a foreground session only once`() {
        val events =
            listOf(
                UsageActivityEvent(100L, "com.example", "MainActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(900L, "com.example", "MainActivity", UsageActivityEventType.PAUSED),
            )

        val firstWindow = aggregateForegroundUsage(events.filter { it.timestamp < 500L }, 0L, 500L)
        val secondWindow = aggregateForegroundUsage(events, 500L, 1_000L)

        assertEquals(400L, firstWindow.getValue("com.example"))
        assertEquals(400L, secondWindow.getValue("com.example"))
    }

    @Test
    fun `session active at collection boundary is clipped to requested window`() {
        val events =
            listOf(
                UsageActivityEvent(100L, "com.example", "MainActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(900L, "com.example", "MainActivity", UsageActivityEventType.PAUSED),
            )

        assertEquals(
            400L,
            aggregateForegroundUsage(events, 500L, 900L).getValue("com.example"),
        )
    }

    @Test
    fun `event at exclusive end boundary belongs to the next window`() {
        val events =
            listOf(
                UsageActivityEvent(100L, "com.example", "MainActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(500L, "com.example", "MainActivity", UsageActivityEventType.PAUSED),
            )

        assertEquals(
            400L,
            aggregateForegroundUsage(events.filter { it.timestamp < 500L }, 0L, 500L).getValue("com.example"),
        )
        assertEquals(emptyMap<String, Long>(), aggregateForegroundUsage(events, 500L, 1_000L))
    }

    @Test
    fun `overlapping resumed packages cannot exceed elapsed wall time`() {
        val events =
            listOf(
                UsageActivityEvent(100L, "com.android.launcher3", "Launcher", UsageActivityEventType.RESUMED),
                UsageActivityEvent(200L, "com.example", "MainActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(800L, "com.example", "MainActivity", UsageActivityEventType.PAUSED),
                UsageActivityEvent(900L, "com.android.launcher3", "Launcher", UsageActivityEventType.PAUSED),
            )

        val usage = aggregateForegroundUsage(events, 100L, 900L)

        assertEquals(800L, usage.values.sum())
        assertEquals(200L, usage.getValue("com.android.launcher3"))
        assertEquals(600L, usage.getValue("com.example"))
    }

    @Test
    fun `uninstalled system and launcher package names remain in usage results`() {
        val events =
            listOf(
                UsageActivityEvent(100L, "com.removed.app", "RemovedActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(200L, "com.removed.app", "RemovedActivity", UsageActivityEventType.PAUSED),
                UsageActivityEvent(300L, "android", "ResolverActivity", UsageActivityEventType.RESUMED),
                UsageActivityEvent(400L, "android", "ResolverActivity", UsageActivityEventType.PAUSED),
                UsageActivityEvent(500L, "com.android.launcher3", "Launcher", UsageActivityEventType.RESUMED),
                UsageActivityEvent(600L, "com.android.launcher3", "Launcher", UsageActivityEventType.PAUSED),
            )

        val usage = aggregateForegroundUsage(events, 0L, 1_000L)

        assertEquals(100L, usage.getValue("com.removed.app"))
        assertEquals(100L, usage.getValue("android"))
        assertEquals(100L, usage.getValue("com.android.launcher3"))
    }
}
