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
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
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

internal data class QuickGlancePresentation(
    val layout: QuickGlanceLayout,
    val outerPaddingDp: Int,
    val cellPaddingDp: Int,
    val valueFontSp: Int,
    val labelFontSp: Int,
    val maxDisplayCharacters: Int,
    val valueMaxLines: Int = 1,
    val labelMaxLines: Int = 1,
) {
    fun availableCellHeightDp(size: DpSize): Float =
        (size.height.value - outerPaddingDp * 2f) / QUICK_GLANCE_ROW_COUNT

    fun requiredCellContentHeightDp(fontScale: Float): Float =
        cellPaddingDp * 2f +
            (valueFontSp + labelFontSp) * fontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER
}

internal data class QuickGlanceCellValue(
    val label: String,
    val value: String,
)

internal data class QuickGlanceCellModel(
    val metric: QuickGlanceMetric,
    val displayLabel: String,
    val displayValue: String,
    val accessibilityLabel: String,
)

internal fun quickGlanceLayoutFor(size: DpSize): QuickGlanceLayout =
    when {
        size.width >= 320.dp && size.height >= 180.dp -> QuickGlanceLayout.EXPANDED
        size.width >= 250.dp && size.height >= 110.dp -> QuickGlanceLayout.STANDARD
        else -> QuickGlanceLayout.COMPACT
    }

internal fun quickGlancePresentationFor(
    size: DpSize,
    fontScale: Float,
): QuickGlancePresentation {
    val layout = quickGlanceLayoutFor(size)
    val typography =
        when {
            fontScale >= 1.75f && layout == QuickGlanceLayout.EXPANDED -> Triple(14, 10, 18)
            fontScale >= 1.75f -> Triple(8, 7, 12)
            fontScale >= 1.2f && layout == QuickGlanceLayout.EXPANDED -> Triple(16, 10, 18)
            fontScale >= 1.2f -> Triple(12, 8, 14)
            layout == QuickGlanceLayout.EXPANDED -> Triple(18, 10, 20)
            layout == QuickGlanceLayout.STANDARD -> Triple(16, 10, 18)
            else -> Triple(15, 9, 16)
        }
    return QuickGlancePresentation(
        layout = layout,
        outerPaddingDp = 12,
        cellPaddingDp = 4,
        valueFontSp = typography.first,
        labelFontSp = typography.second,
        maxDisplayCharacters = typography.third,
    )
}

internal fun quickGlanceCellModels(
    values: Map<QuickGlanceMetric, QuickGlanceCellValue>,
    presentation: QuickGlancePresentation,
): List<QuickGlanceCellModel> =
    QuickGlanceMetric.entries.map { metric ->
        val cell = requireNotNull(values[metric]) { "Missing Quick Glance value for $metric" }
        QuickGlanceCellModel(
            metric = metric,
            displayLabel = cell.label.ellipsizeForWidget(presentation.maxDisplayCharacters),
            displayValue = cell.value.ellipsizeForWidget(presentation.maxDisplayCharacters),
            accessibilityLabel = "${cell.label}, ${cell.value}",
        )
    }

private fun String.ellipsizeForWidget(maxCharacters: Int): String =
    if (length <= maxCharacters) this else take(maxCharacters - 1) + "…"

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
    val presentation =
        quickGlancePresentationFor(
            size = LocalSize.current,
            fontScale = context.resources.configuration.fontScale,
        )
    val models =
        quickGlanceCellModels(
            values =
                mapOf(
                    QuickGlanceMetric.HEALTH to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_health_score_label),
                            value = healthValue,
                        ),
                    QuickGlanceMetric.BATTERY to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_battery_label),
                            value = batteryValue,
                        ),
                    QuickGlanceMetric.STORAGE to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_free_storage_label),
                            value = storageValue,
                        ),
                    QuickGlanceMetric.TEMPERATURE to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_temperature_label),
                            value = temperatureValue,
                        ),
                ),
            presentation = presentation,
        )

    RuncheckWidgetTheme {
        Column(
            modifier = widgetSurfaceModifier(presentation.outerPaddingDp.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    model = models[0],
                    presentation = presentation,
                )
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    model = models[1],
                    presentation = presentation,
                )
            }
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    model = models[2],
                    presentation = presentation,
                )
                QuickGlanceCell(
                    modifier = GlanceModifier.defaultWeight(),
                    context = context,
                    model = models[3],
                    presentation = presentation,
                )
            }
        }
    }
}

@Composable
private fun QuickGlanceCell(
    modifier: GlanceModifier,
    context: Context,
    model: QuickGlanceCellModel,
    presentation: QuickGlancePresentation,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(actionStartActivity(widgetNavigationIntent(context, model.metric.route)))
                .semantics {
                    contentDescription = model.accessibilityLabel
                    testTag = "quick_glance_${model.metric.name.lowercase()}"
                }
                .padding(presentation.cellPaddingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = model.displayValue,
            maxLines = presentation.valueMaxLines,
            style =
                TextStyle(
                    fontSize = presentation.valueFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
        )
        Text(
            text = model.displayLabel,
            maxLines = presentation.labelMaxLines,
            style =
                TextStyle(
                    fontSize = presentation.labelFontSp.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }
}

class QuickGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickGlanceWidget()
}

private const val QUICK_GLANCE_ROW_COUNT = 2f
