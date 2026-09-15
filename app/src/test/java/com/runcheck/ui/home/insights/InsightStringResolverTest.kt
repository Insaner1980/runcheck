package com.runcheck.ui.home.insights

import android.content.Context
import com.runcheck.R
import com.runcheck.domain.insights.model.InsightMessageId
import com.runcheck.testutil.findAppDir
import com.runcheck.testutil.insightFixture
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.w3c.dom.Element
import java.util.Locale
import java.util.MissingFormatArgumentException
import javax.xml.parsers.DocumentBuilderFactory

class InsightStringResolverTest {
    // Exercise the resolver against current English XML with JVM formatting, without an Android runtime.
    private val strings = loadStrings()
    private val context = mockk<Context>()

    init {
        every { context.getString(any(), *anyVararg()) } answers {
            String.format(Locale.ENGLISH, strings.getValue(firstArg()), *secondArg<Array<Any>>())
        }
    }

    private val expected =
        mapOf(
            InsightMessageId.BATTERY_DEGRADATION to
                InsightMessageResources(
                    R.string.insight_battery_degradation_title,
                    R.string.insight_battery_degradation_body,
                ),
            InsightMessageId.BATTERY_BASELINE_ANOMALY to
                InsightMessageResources(
                    R.string.insight_battery_baseline_anomaly_title,
                    R.string.insight_battery_baseline_anomaly_body,
                ),
            InsightMessageId.CHARGER_PERFORMANCE to
                InsightMessageResources(
                    R.string.insight_charger_performance_title,
                    R.string.insight_charger_performance_body,
                ),
            InsightMessageId.HEAVY_APP_USAGE to
                InsightMessageResources(
                    R.string.insight_app_usage_title,
                    R.string.insight_app_usage_body,
                ),
            InsightMessageId.NETWORK_SIGNAL_PATTERN to
                InsightMessageResources(
                    R.string.insight_network_signal_pattern_title,
                    R.string.insight_network_signal_pattern_body,
                ),
            InsightMessageId.NETWORK_DRIVEN_BATTERY_DRAIN to
                InsightMessageResources(
                    R.string.insight_network_drain_title,
                    R.string.insight_network_drain_body,
                ),
            InsightMessageId.HEAT_ACCELERATED_BATTERY_WEAR to
                InsightMessageResources(
                    R.string.insight_heat_battery_wear_title,
                    R.string.insight_heat_battery_wear_body,
                ),
            InsightMessageId.STORAGE_PRESSURE_PROJECTION to
                InsightMessageResources(
                    R.string.insight_storage_pressure_title,
                    R.string.insight_storage_pressure_body,
                ),
            InsightMessageId.STORAGE_PRESSURE_IMPACT to
                InsightMessageResources(
                    R.string.insight_storage_impact_title,
                    R.string.insight_storage_impact_body,
                ),
            InsightMessageId.RECURRING_THERMAL_THROTTLING to
                InsightMessageResources(
                    R.string.insight_thermal_throttling_title,
                    R.string.insight_thermal_throttling_body,
                ),
            InsightMessageId.THERMAL_PATTERN to
                InsightMessageResources(
                    R.string.insight_thermal_pattern_title,
                    R.string.insight_thermal_pattern_body,
                ),
            InsightMessageId.LEGACY_APP_BATTERY_IMPACT to
                InsightMessageResources(
                    R.string.insight_app_battery_impact_title,
                    R.string.insight_app_battery_impact_body,
                ),
        )

    @Test
    fun `every active and legacy id has the exact resource pair`() {
        assertEquals(expected.keys, InsightMessageId.entries.toSet())
        InsightMessageId.entries.forEach { id ->
            assertEquals(expected.getValue(id), id.resources())
            val resources = expected.getValue(id)
            val bodyArgs = listOf("first", "second", "third")
            val actual = resolve(id.titleKey, id.bodyKey, bodyArgs)
            assertEquals(strings.getValue(resources.titleRes), actual.title)
            assertEquals(
                String.format(Locale.ENGLISH, strings.getValue(resources.bodyRes), *bodyArgs.toTypedArray()),
                actual.body,
            )
        }
    }

    @Test
    fun `active pair formats body arguments exactly`() {
        val actual = resolve("insight_battery_degradation_title", "insight_battery_degradation_body", listOf("25"))
        assertEquals("Battery draining faster", actual.title)
        assertEquals("Your battery has been draining 25% faster this week compared to last week.", actual.body)
    }

    @Test
    fun `legacy pair resolves all three historical arguments`() {
        val actual =
            resolve(
                "insight_app_battery_impact_title",
                "insight_app_battery_impact_body",
                listOf("VideoApp", "120", "45"),
            )
        assertEquals("One app is hitting battery unusually hard", actual.title)
        assertEquals(
            "VideoApp accounted for about 120 mAh of estimated drain, around 45% of tracked app battery use today.",
            actual.body,
        )
    }

    @Test
    fun `unknown title retains raw key while known body formats`() {
        val actual = resolve(" Unknown Title ", "insight_battery_degradation_body", listOf("25"))
        assertEquals(" Unknown Title ", actual.title)
        assertEquals("Your battery has been draining 25% faster this week compared to last week.", actual.body)
    }

    @Test
    fun `unknown body retains raw key without appending arguments`() {
        val actual = resolve("insight_battery_degradation_title", " Unknown Body ", listOf("25", "extra"))
        assertEquals("Battery draining faster", actual.title)
        assertEquals(" Unknown Body ", actual.body)
    }

    @Test
    fun `unknown pair retains both raw keys`() {
        val actual = resolve("unknown_title", "unknown_body", listOf("ignored"))
        assertEquals("unknown_title", actual.title)
        assertEquals("unknown_body", actual.body)
    }

    @Test
    fun `mismatched known keys retain independent localization`() {
        val actual = resolve("insight_battery_degradation_title", "insight_storage_pressure_body", listOf("3d"))
        assertEquals("Battery draining faster", actual.title)
        assertEquals("At the current growth rate, free space could run out in about 3d.", actual.body)
    }

    @Test(expected = MissingFormatArgumentException::class)
    fun `known body formatting exceptions still propagate`() {
        resolve("insight_app_battery_impact_title", "insight_app_battery_impact_body", listOf("VideoApp"))
    }

    private fun resolve(
        titleKey: String,
        bodyKey: String,
        bodyArgs: List<String>,
    ): InsightMessageText =
        resolveInsightMessage(
            context,
            insightFixture().copy(titleKey = titleKey, bodyKey = bodyKey, bodyArgs = bodyArgs),
        )

    private fun loadStrings(): Map<Int, String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(findAppDir().resolve("src/main/res/values/strings.xml").toFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .map { nodes.item(it) as Element }
            .filter { it.getAttribute("name").startsWith("insight_") }
            .associate { element ->
                R.string::class.java.getField(element.getAttribute("name")).getInt(null) to element.textContent
            }
    }
}
