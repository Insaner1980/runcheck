package com.runcheck.widget

import android.text.format.Formatter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
            quickGlanceLayoutFor(DpSize(180.dp, 120.dp)),
        )
        assertEquals(
            QuickGlanceLayout.STANDARD,
            quickGlanceLayoutFor(DpSize(320.dp, 120.dp)),
        )
        assertEquals(
            QuickGlanceLayout.EXPANDED,
            quickGlanceLayoutFor(DpSize(600.dp, 240.dp)),
        )
    }

    @Test
    fun `all widget responsive policies remain usable at their declared minimum launcher size`() {
        assertEquals(WidgetLayout.COMPACT, batteryWidgetLayoutFor(DpSize(110.dp, 72.dp)))
        assertEquals(WidgetLayout.COMPACT, healthWidgetLayoutFor(DpSize(110.dp, 110.dp)))
        assertEquals(QuickGlanceLayout.COMPACT, quickGlanceLayoutFor(DpSize(180.dp, 120.dp)))
    }

    @Test
    fun `quick glance typography fits compact and landscape cells at supported font scales`() {
        val sizes =
            listOf(
                DpSize(180.dp, 120.dp),
                DpSize(320.dp, 120.dp),
                DpSize(600.dp, 240.dp),
            )

        sizes.forEach { size ->
            listOf(1f, 1.3f, 2f).forEach { fontScale ->
                val presentation = quickGlancePresentationFor(size, fontScale)
                assertTrue(
                    "$size at $fontScale must preserve a 48dp cell",
                    presentation.availableCellHeightDp(size) >= 48f,
                )
                assertTrue(
                    "$size at $fontScale must fit text inside the cell",
                    presentation.requiredCellContentHeightDp(fontScale) <=
                        presentation.availableCellHeightDp(size),
                )
                assertEquals(1, presentation.valueMaxLines)
                assertEquals(1, presentation.labelMaxLines)
            }
        }
    }

    @Test
    fun `actual quick glance metric copy fits the available cell width at supported sizes and font scales`() {
        val context = RuntimeEnvironment.getApplication()
        val values = productionMetricValues()
        assertEquals("80 · Healthy", values.getValue(QuickGlanceMetric.HEALTH).value)

        listOf(
            DpSize(180.dp, 120.dp),
            DpSize(320.dp, 120.dp),
            DpSize(600.dp, 240.dp),
        ).forEach { size ->
            listOf(1f, 1.3f, 2f).forEach { fontScale ->
                val presentation = quickGlancePresentationFor(size, fontScale)
                val models = quickGlanceCellModels(values, presentation)
                val widthBudget = presentation.availableCellTextWidthDp(size)

                models.forEach { model ->
                    assertTrue(
                        "${model.metric} value must fit $widthBudget dp at $size / $fontScale",
                        estimateWidgetTextWidthDp(
                            text = model.displayValue,
                            fontSizeSp = model.valueFontSp,
                            fontScale = fontScale,
                        ) <= widthBudget,
                    )
                    assertTrue(
                        "${model.metric} label must fit $widthBudget dp at $size / $fontScale",
                        estimateWidgetTextWidthDp(
                            text = model.displayLabel,
                            fontSizeSp = model.labelFontSp,
                            fontScale = fontScale,
                        ) <= widthBudget,
                    )
                    assertFalse(model.displayValue.endsWith("…"))
                    assertFalse(model.displayLabel.endsWith("…"))
                }

                val health = models.first()
                val visibleHealthCopy = "${health.displayLabel} ${health.displayValue}"
                assertTrue(visibleHealthCopy.contains("80"))
                assertTrue(visibleHealthCopy.contains(context.getString(R.string.status_healthy)))
                assertEquals(
                    "Health Score, 80 · Healthy",
                    health.accessibilityLabel,
                )
            }
        }
    }

    @Test
    fun `quick glance models preserve row major TalkBack labels while visible text ellipsizes`() {
        val presentation = quickGlancePresentationFor(DpSize(180.dp, 120.dp), fontScale = 2f)
        val models =
            quickGlanceCellModels(
                values =
                    mapOf(
                        QuickGlanceMetric.HEALTH to
                            QuickGlanceCellValue("Health", "80 Healthy"),
                        QuickGlanceMetric.BATTERY to
                            QuickGlanceCellValue("Battery", "80 percent"),
                        QuickGlanceMetric.STORAGE to
                            QuickGlanceCellValue("Free storage", "12345678901234567890 GB"),
                        QuickGlanceMetric.TEMPERATURE to
                            QuickGlanceCellValue("Temperature", "25 degrees Celsius"),
                    ),
                presentation = presentation,
            )

        assertEquals(QuickGlanceMetric.entries, models.map(QuickGlanceCellModel::metric))
        assertEquals(
            listOf(
                "Health, 80 Healthy",
                "Battery, 80 percent",
                "Free storage, 12345678901234567890 GB",
                "Temperature, 25 degrees Celsius",
            ),
            models.map(QuickGlanceCellModel::accessibilityLabel),
        )
        assertTrue(models[2].displayValue.endsWith("…"))
        assertTrue(models[3].displayValue.endsWith("…"))
        assertTrue(
            "${models.first().displayLabel} ${models.first().displayValue}".contains("Healthy"),
        )
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

    private fun productionMetricValues(): Map<QuickGlanceMetric, QuickGlanceCellValue> {
        val context = RuntimeEnvironment.getApplication()
        val healthy = context.getString(R.string.status_healthy)
        return mapOf(
            QuickGlanceMetric.HEALTH to
                QuickGlanceCellValue(
                    label = context.getString(R.string.widget_health_score_label),
                    value = context.getString(R.string.widget_score_with_status, 80, healthy),
                    compactLabel = context.getString(R.string.widget_health_compact_label),
                ),
            QuickGlanceMetric.BATTERY to
                QuickGlanceCellValue(
                    label = context.getString(R.string.widget_battery_label),
                    value = context.getString(R.string.widget_percent_value, 80),
                    compactLabel = context.getString(R.string.widget_battery_label),
                ),
            QuickGlanceMetric.STORAGE to
                QuickGlanceCellValue(
                    label = context.getString(R.string.widget_free_storage_label),
                    value = Formatter.formatShortFileSize(context, 123_000_000_000L),
                    compactLabel = context.getString(R.string.widget_free_storage_compact_label),
                ),
            QuickGlanceMetric.TEMPERATURE to
                QuickGlanceCellValue(
                    label = context.getString(R.string.widget_temperature_label),
                    value = context.getString(R.string.widget_temperature_value, 25f),
                    compactLabel = context.getString(R.string.widget_temperature_compact_label),
                ),
        )
    }
}
