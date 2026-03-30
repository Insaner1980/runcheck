package com.runcheck.ui.home.insights

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.domain.insights.model.Insight

private val insightTitleResources =
    mapOf(
        "insight_battery_degradation_title" to R.string.insight_battery_degradation_title,
        "insight_charger_performance_title" to R.string.insight_charger_performance_title,
        "insight_app_battery_impact_title" to R.string.insight_app_battery_impact_title,
        "insight_storage_pressure_title" to R.string.insight_storage_pressure_title,
        "insight_thermal_pattern_title" to R.string.insight_thermal_pattern_title,
        "insight_thermal_throttling_title" to R.string.insight_thermal_throttling_title,
        "insight_app_usage_title" to R.string.insight_app_usage_title,
        "insight_network_signal_pattern_title" to R.string.insight_network_signal_pattern_title,
        "insight_network_drain_title" to R.string.insight_network_drain_title,
        "insight_heat_battery_wear_title" to R.string.insight_heat_battery_wear_title,
        "insight_storage_impact_title" to R.string.insight_storage_impact_title,
    )

private val insightBodyResources =
    mapOf(
        "insight_battery_degradation_body" to R.string.insight_battery_degradation_body,
        "insight_charger_performance_body" to R.string.insight_charger_performance_body,
        "insight_app_battery_impact_body" to R.string.insight_app_battery_impact_body,
        "insight_storage_pressure_body" to R.string.insight_storage_pressure_body,
        "insight_thermal_pattern_body" to R.string.insight_thermal_pattern_body,
        "insight_thermal_throttling_body" to R.string.insight_thermal_throttling_body,
        "insight_app_usage_body" to R.string.insight_app_usage_body,
        "insight_network_signal_pattern_body" to R.string.insight_network_signal_pattern_body,
        "insight_network_drain_body" to R.string.insight_network_drain_body,
        "insight_heat_battery_wear_body" to R.string.insight_heat_battery_wear_body,
        "insight_storage_impact_body" to R.string.insight_storage_impact_body,
    )

@Composable
internal fun resolveInsightTitle(insight: Insight): String =
    resolveInsightString(insight.titleKey, insightTitleResources)

@Composable
internal fun resolveInsightBody(insight: Insight): String =
    resolveInsightString(
        key = insight.bodyKey,
        resources = insightBodyResources,
        args = insight.bodyArgs.toTypedArray(),
    )

@Composable
private fun resolveInsightString(
    key: String,
    resources: Map<String, Int>,
    vararg args: Any,
): String {
    val resourceId = resources[key] ?: return key
    return stringResource(resourceId, *args)
}
