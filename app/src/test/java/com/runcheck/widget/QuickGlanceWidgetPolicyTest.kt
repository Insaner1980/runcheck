package com.runcheck.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuickGlanceWidgetPolicyTest {
    @Test
    fun `quick glance cells use the documented direct routes in TalkBack order`() {
        assertEquals(
            listOf(
                Screen.Home.route,
                Screen.Battery.route,
                Screen.Storage.route,
                Screen.Thermal.route,
            ),
            QuickGlanceMetric.entries.map(QuickGlanceMetric::route),
        )
    }

    @Test
    fun `quick glance chooses compact standard and expanded layouts for launcher sizes`() {
        assertEquals(
            QuickGlanceLayout.COMPACT,
            quickGlanceLayoutFor(DpSize(180.dp, 110.dp)),
        )
        assertEquals(
            QuickGlanceLayout.STANDARD,
            quickGlanceLayoutFor(DpSize(250.dp, 110.dp)),
        )
        assertEquals(
            QuickGlanceLayout.EXPANDED,
            quickGlanceLayoutFor(DpSize(320.dp, 180.dp)),
        )
    }

    @Test
    fun `all widget responsive policies remain usable at their minimum launcher size`() {
        assertEquals(WidgetLayout.COMPACT, batteryWidgetLayoutFor(DpSize(110.dp, 40.dp)))
        assertEquals(WidgetLayout.COMPACT, healthWidgetLayoutFor(DpSize(110.dp, 110.dp)))
        assertEquals(QuickGlanceLayout.COMPACT, quickGlanceLayoutFor(DpSize(180.dp, 110.dp)))
    }

    @Test
    fun `quick glance cell intents have distinct identities and route extras`() {
        val context = RuntimeEnvironment.getApplication()
        val intents =
            QuickGlanceMetric.entries.map { metric ->
                metric to widgetNavigationIntent(context, metric.route)
            }

        assertEquals(intents.size, intents.map { it.second.action }.distinct().size)
        intents.forEach { (metric, intent) ->
            assertEquals(metric.route, intent.getStringExtra(NotificationHelper.EXTRA_NAVIGATE_TO))
        }
    }
}
