package com.runcheck.domain.insights.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightMessageIdTest {
    @Test
    fun `catalog preserves every explicit active and legacy key pair`() {
        val expected =
            mapOf(
                InsightMessageId.BATTERY_DEGRADATION to
                    ("insight_battery_degradation_title" to "insight_battery_degradation_body"),
                InsightMessageId.BATTERY_BASELINE_ANOMALY to
                    ("insight_battery_baseline_anomaly_title" to "insight_battery_baseline_anomaly_body"),
                InsightMessageId.CHARGER_PERFORMANCE to
                    ("insight_charger_performance_title" to "insight_charger_performance_body"),
                InsightMessageId.HEAVY_APP_USAGE to
                    ("insight_app_usage_title" to "insight_app_usage_body"),
                InsightMessageId.NETWORK_SIGNAL_PATTERN to
                    ("insight_network_signal_pattern_title" to "insight_network_signal_pattern_body"),
                InsightMessageId.NETWORK_DRIVEN_BATTERY_DRAIN to
                    ("insight_network_drain_title" to "insight_network_drain_body"),
                InsightMessageId.HEAT_ACCELERATED_BATTERY_WEAR to
                    ("insight_heat_battery_wear_title" to "insight_heat_battery_wear_body"),
                InsightMessageId.STORAGE_PRESSURE_PROJECTION to
                    ("insight_storage_pressure_title" to "insight_storage_pressure_body"),
                InsightMessageId.STORAGE_PRESSURE_IMPACT to
                    ("insight_storage_impact_title" to "insight_storage_impact_body"),
                InsightMessageId.RECURRING_THERMAL_THROTTLING to
                    ("insight_thermal_throttling_title" to "insight_thermal_throttling_body"),
                InsightMessageId.THERMAL_PATTERN to
                    ("insight_thermal_pattern_title" to "insight_thermal_pattern_body"),
                InsightMessageId.LEGACY_APP_BATTERY_IMPACT to
                    ("insight_app_battery_impact_title" to "insight_app_battery_impact_body"),
            )

        assertEquals(expected.keys, InsightMessageId.entries.toSet())
        expected.forEach { (id, keys) ->
            assertEquals(keys.first, id.titleKey)
            assertEquals(keys.second, id.bodyKey)
            assertEquals(id, InsightMessageId.fromKeys(keys.first, keys.second))
        }
    }

    @Test
    fun `title keys are unique`() {
        assertEquals(
            InsightMessageId.entries.size,
            InsightMessageId.entries
                .map { it.titleKey }
                .toSet()
                .size,
        )
    }

    @Test
    fun `body keys are unique`() {
        assertEquals(
            InsightMessageId.entries.size,
            InsightMessageId.entries
                .map { it.bodyKey }
                .toSet()
                .size,
        )
    }

    @Test
    fun `title and body keys never collide`() {
        val titles = InsightMessageId.entries.map { it.titleKey }.toSet()
        val bodies = InsightMessageId.entries.map { it.bodyKey }.toSet()
        assertTrue(titles.intersect(bodies).isEmpty())
    }

    @Test
    fun `decoder rejects mismatched and unknown pairs`() {
        InsightMessageId.entries.forEach { titleId ->
            InsightMessageId.entries.filter { it != titleId }.forEach { bodyId ->
                assertNull(InsightMessageId.fromKeys(titleId.titleKey, bodyId.bodyKey))
            }
            assertNull(InsightMessageId.fromKeys(titleId.titleKey, "unknown_body"))
            assertNull(InsightMessageId.fromKeys("unknown_title", titleId.bodyKey))
        }
        assertNull(InsightMessageId.fromKeys("unknown_title", "unknown_body"))
    }

    @Test
    fun `decoder does not normalize either key`() {
        InsightMessageId.entries.forEach { id ->
            listOf(id.titleKey.uppercase(), " ${id.titleKey}", "${id.titleKey} ").forEach { title ->
                assertNull(InsightMessageId.fromKeys(title, id.bodyKey))
            }
            listOf(id.bodyKey.uppercase(), " ${id.bodyKey}", "${id.bodyKey} ").forEach { body ->
                assertNull(InsightMessageId.fromKeys(id.titleKey, body))
            }
        }
    }
}
