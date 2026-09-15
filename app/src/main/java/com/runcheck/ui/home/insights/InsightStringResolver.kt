package com.runcheck.ui.home.insights

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.runcheck.R.string.insight_app_battery_impact_body
import com.runcheck.R.string.insight_app_battery_impact_title
import com.runcheck.R.string.insight_app_usage_body
import com.runcheck.R.string.insight_app_usage_title
import com.runcheck.R.string.insight_battery_baseline_anomaly_body
import com.runcheck.R.string.insight_battery_baseline_anomaly_title
import com.runcheck.R.string.insight_battery_degradation_body
import com.runcheck.R.string.insight_battery_degradation_title
import com.runcheck.R.string.insight_charger_performance_body
import com.runcheck.R.string.insight_charger_performance_title
import com.runcheck.R.string.insight_heat_battery_wear_body
import com.runcheck.R.string.insight_heat_battery_wear_title
import com.runcheck.R.string.insight_network_drain_body
import com.runcheck.R.string.insight_network_drain_title
import com.runcheck.R.string.insight_network_signal_pattern_body
import com.runcheck.R.string.insight_network_signal_pattern_title
import com.runcheck.R.string.insight_storage_impact_body
import com.runcheck.R.string.insight_storage_impact_title
import com.runcheck.R.string.insight_storage_pressure_body
import com.runcheck.R.string.insight_storage_pressure_title
import com.runcheck.R.string.insight_thermal_pattern_body
import com.runcheck.R.string.insight_thermal_pattern_title
import com.runcheck.R.string.insight_thermal_throttling_body
import com.runcheck.R.string.insight_thermal_throttling_title
import com.runcheck.domain.insights.model.Insight
import com.runcheck.domain.insights.model.InsightMessageId

internal data class InsightMessageResources(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

internal fun InsightMessageId.resources(): InsightMessageResources =
    when (this) {
        InsightMessageId.BATTERY_DEGRADATION -> {
            InsightMessageResources(insight_battery_degradation_title, insight_battery_degradation_body)
        }

        InsightMessageId.BATTERY_BASELINE_ANOMALY -> {
            InsightMessageResources(insight_battery_baseline_anomaly_title, insight_battery_baseline_anomaly_body)
        }

        InsightMessageId.CHARGER_PERFORMANCE -> {
            InsightMessageResources(insight_charger_performance_title, insight_charger_performance_body)
        }

        InsightMessageId.HEAVY_APP_USAGE -> {
            InsightMessageResources(insight_app_usage_title, insight_app_usage_body)
        }

        InsightMessageId.NETWORK_SIGNAL_PATTERN -> {
            InsightMessageResources(insight_network_signal_pattern_title, insight_network_signal_pattern_body)
        }

        InsightMessageId.NETWORK_DRIVEN_BATTERY_DRAIN -> {
            InsightMessageResources(insight_network_drain_title, insight_network_drain_body)
        }

        InsightMessageId.HEAT_ACCELERATED_BATTERY_WEAR -> {
            InsightMessageResources(insight_heat_battery_wear_title, insight_heat_battery_wear_body)
        }

        InsightMessageId.STORAGE_PRESSURE_PROJECTION -> {
            InsightMessageResources(insight_storage_pressure_title, insight_storage_pressure_body)
        }

        InsightMessageId.STORAGE_PRESSURE_IMPACT -> {
            InsightMessageResources(insight_storage_impact_title, insight_storage_impact_body)
        }

        InsightMessageId.RECURRING_THERMAL_THROTTLING -> {
            InsightMessageResources(insight_thermal_throttling_title, insight_thermal_throttling_body)
        }

        InsightMessageId.THERMAL_PATTERN -> {
            InsightMessageResources(insight_thermal_pattern_title, insight_thermal_pattern_body)
        }

        InsightMessageId.LEGACY_APP_BATTERY_IMPACT -> {
            InsightMessageResources(insight_app_battery_impact_title, insight_app_battery_impact_body)
        }
    }

internal data class InsightMessageText(
    val title: String,
    val body: String,
)

@Composable
internal fun resolveInsightMessage(insight: Insight): InsightMessageText =
    resolveInsightMessage(LocalContext.current, insight)

internal fun resolveInsightMessage(
    context: Context,
    insight: Insight,
): InsightMessageText {
    val canonical = InsightMessageId.fromKeys(insight.titleKey, insight.bodyKey)?.resources()
    // Historical rows retain independent per-key resolution when the pair is not canonical.
    val titleRes =
        canonical?.titleRes
            ?: InsightMessageId.entries
                .firstOrNull { it.titleKey == insight.titleKey }
                ?.resources()
                ?.titleRes
    val bodyRes =
        canonical?.bodyRes
            ?: InsightMessageId.entries
                .firstOrNull { it.bodyKey == insight.bodyKey }
                ?.resources()
                ?.bodyRes
    return InsightMessageText(
        title = titleRes?.let { context.getString(it, *emptyArray<Any>()) } ?: insight.titleKey,
        body = bodyRes?.let { context.getString(it, *insight.bodyArgs.toTypedArray()) } ?: insight.bodyKey,
    )
}
