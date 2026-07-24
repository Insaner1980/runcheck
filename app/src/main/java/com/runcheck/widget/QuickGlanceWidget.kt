package com.runcheck.widget

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.ui.common.healthStatusLabelRes
import com.runcheck.ui.navigation.Screen

internal enum class QuickGlanceMetric(
    val route: String,
) {
    HEALTH(Screen.Home.route),
    BATTERY(Screen.Battery.route),
    STORAGE(Screen.Storage.route),
    TEMPERATURE(Screen.Thermal.route),
}

internal enum class QuickGlanceLayout {
    COMPACT,
    STANDARD,
    EXPANDED,
}

internal fun quickGlanceLayoutFor(size: DpSize): QuickGlanceLayout =
    when {
        size.width >= 320.dp && size.height >= 180.dp -> QuickGlanceLayout.EXPANDED
        size.width >= 250.dp && size.height >= 110.dp -> QuickGlanceLayout.STANDARD
        else -> QuickGlanceLayout.COMPACT
    }

class QuickGlanceWidget : GlanceAppWidget() {
    companion object {
        private val COMPACT = DpSize(180.dp, 120.dp)
        private val STANDARD = DpSize(250.dp, 120.dp)
        private val EXPANDED = DpSize(320.dp, 180.dp)
    }

    override val sizeMode =
        SizeMode.Responsive(
            setOf(COMPACT, STANDARD, EXPANDED),
        )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val widgetState by
                WidgetDataProvider
                    .observeHealthWidgetState(context)
                    .collectAsState(initial = WidgetRenderState.Loading)

            when (val state = widgetState) {
                WidgetRenderState.Empty -> WidgetEmptyContent(context, Screen.Home.route)
                WidgetRenderState.Loading -> WidgetLoadingContent(context, Screen.Home.route)
                WidgetRenderState.Locked -> WidgetLockedContent(context, R.string.widget_quick_glance_name)
                WidgetRenderState.Stale -> WidgetStaleContent(context, Screen.Home.route)
                WidgetRenderState.Unavailable -> WidgetUnavailableContent(context, Screen.Home.route)
                is WidgetRenderState.Content -> QuickGlanceContent(context, state.snapshot)
            }
        }
    }
}

@Composable
private fun QuickGlanceContent(
    context: Context,
    snapshot: HealthWidgetSnapshot,
) {
    val status = HealthScore.statusFromScore(snapshot.overallScore)
    val healthStatus = context.getString(healthStatusLabelRes(status))
    val healthValue = context.getString(R.string.widget_score_with_status, snapshot.overallScore, healthStatus)
    val batteryValue = context.getString(R.string.widget_percent_value, snapshot.batteryLevel)
    val storageValue = Formatter.formatShortFileSize(context, snapshot.availableStorageBytes)
    val temperatureValue = context.getString(R.string.widget_temperature_value, snapshot.temperatureC)
    val layout = quickGlanceLayoutFor(LocalSize.current)
    val valueSize = if (layout == QuickGlanceLayout.EXPANDED) 18.sp else 15.sp
    val labelSize = if (layout == QuickGlanceLayout.COMPACT) 9.sp else 10.sp

    RuncheckWidgetTheme {
        Column(
            modifier = widgetSurfaceModifier(),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    metric = QuickGlanceMetric.HEALTH,
                    label = context.getString(R.string.widget_health_score_label),
                    value = healthValue,
                    valueSize = valueSize,
                    labelSize = labelSize,
                )
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    metric = QuickGlanceMetric.BATTERY,
                    label = context.getString(R.string.widget_battery_label),
                    value = batteryValue,
                    valueSize = valueSize,
                    labelSize = labelSize,
                )
            }
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    metric = QuickGlanceMetric.STORAGE,
                    label = context.getString(R.string.widget_free_storage_label),
                    value = storageValue,
                    valueSize = valueSize,
                    labelSize = labelSize,
                )
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    metric = QuickGlanceMetric.TEMPERATURE,
                    label = context.getString(R.string.widget_temperature_label),
                    value = temperatureValue,
                    valueSize = valueSize,
                    labelSize = labelSize,
                )
            }
        }
    }
}

@Composable
private fun QuickGlanceCell(
    modifier: GlanceModifier,
    context: Context,
    metric: QuickGlanceMetric,
    label: String,
    value: String,
    valueSize: androidx.compose.ui.unit.TextUnit,
    labelSize: androidx.compose.ui.unit.TextUnit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(actionStartActivity(widgetNavigationIntent(context, metric.route)))
                .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            maxLines = 1,
            style =
                TextStyle(
                    fontSize = valueSize,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
        )
        Text(
            text = label,
            maxLines = 1,
            style =
                TextStyle(
                    fontSize = labelSize,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }
}

class QuickGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickGlanceWidget()
}
