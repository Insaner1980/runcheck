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
    val minimumValueFontSp: Int,
    val minimumLabelFontSp: Int,
    val fontScale: Float,
    val cellTextWidthDp: Float,
    val valueMaxLines: Int = 1,
    val labelMaxLines: Int = 1,
) {
    fun availableCellHeightDp(size: DpSize): Float = (size.height.value - outerPaddingDp * 2f) / QUICK_GLANCE_ROW_COUNT

    fun availableCellTextWidthDp(size: DpSize): Float = quickGlanceCellTextWidthDp(size, outerPaddingDp, cellPaddingDp)

    fun requiredCellContentHeightDp(fontScale: Float): Float =
        cellPaddingDp * 2f +
            (valueFontSp + labelFontSp) * fontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER
}

internal data class QuickGlanceCellValue(
    val label: String,
    val value: String,
    val compactLabel: String = label,
)

internal data class QuickGlanceCellModel(
    val metric: QuickGlanceMetric,
    val displayLabel: String,
    val displayValue: String,
    val accessibilityLabel: String,
    val valueFontSp: Int,
    val labelFontSp: Int,
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
            fontScale >= 1.75f && layout == QuickGlanceLayout.EXPANDED -> 14 to 10
            fontScale >= 1.75f && layout == QuickGlanceLayout.STANDARD -> 9 to 7
            fontScale >= 1.75f -> 8 to 7
            fontScale >= 1.2f && layout == QuickGlanceLayout.EXPANDED -> 16 to 10
            fontScale >= 1.2f -> 12 to 8
            layout == QuickGlanceLayout.EXPANDED -> 18 to 10
            layout == QuickGlanceLayout.STANDARD -> 16 to 10
            else -> 15 to 9
        }
    return QuickGlancePresentation(
        layout = layout,
        outerPaddingDp = QUICK_GLANCE_OUTER_PADDING_DP,
        cellPaddingDp = QUICK_GLANCE_CELL_PADDING_DP,
        valueFontSp = typography.first,
        labelFontSp = typography.second,
        minimumValueFontSp = QUICK_GLANCE_MINIMUM_FONT_SP,
        minimumLabelFontSp = QUICK_GLANCE_MINIMUM_FONT_SP,
        fontScale = fontScale,
        cellTextWidthDp =
            quickGlanceCellTextWidthDp(
                size = size,
                outerPaddingDp = QUICK_GLANCE_OUTER_PADDING_DP,
                cellPaddingDp = QUICK_GLANCE_CELL_PADDING_DP,
            ),
    )
}

private fun quickGlanceCellTextWidthDp(
    size: DpSize,
    outerPaddingDp: Int,
    cellPaddingDp: Int,
): Float =
    (size.width.value - outerPaddingDp * 2f) / QUICK_GLANCE_COLUMN_COUNT -
        cellPaddingDp * 2f

internal fun quickGlanceCellModels(
    values: Map<QuickGlanceMetric, QuickGlanceCellValue>,
    presentation: QuickGlancePresentation,
): List<QuickGlanceCellModel> =
    QuickGlanceMetric.entries.map { metric ->
        val cell = requireNotNull(values[metric]) { "Missing Quick Glance value for $metric" }
        val visibleCopy = cell.visibleCopy(metric, presentation.layout)
        val value =
            fitWidgetTextToWidth(
                text = visibleCopy.value,
                preferredFontSizeSp = presentation.valueFontSp,
                minimumFontSizeSp = presentation.minimumValueFontSp,
                fontScale = presentation.fontScale,
                widthDp = presentation.cellTextWidthDp,
            )
        val label =
            fitWidgetTextToWidth(
                text = visibleCopy.label,
                preferredFontSizeSp = presentation.labelFontSp,
                minimumFontSizeSp = presentation.minimumLabelFontSp,
                fontScale = presentation.fontScale,
                widthDp = presentation.cellTextWidthDp,
            )
        QuickGlanceCellModel(
            metric = metric,
            displayLabel = label.text,
            displayValue = value.text,
            accessibilityLabel = "${cell.label}, ${cell.value}",
            valueFontSp = value.fontSizeSp,
            labelFontSp = label.fontSizeSp,
        )
    }

private data class QuickGlanceVisibleCopy(
    val label: String,
    val value: String,
)

private data class FittedWidgetText(
    val text: String,
    val fontSizeSp: Int,
)

private fun QuickGlanceCellValue.visibleCopy(
    metric: QuickGlanceMetric,
    layout: QuickGlanceLayout,
): QuickGlanceVisibleCopy {
    if (layout != QuickGlanceLayout.COMPACT) {
        return QuickGlanceVisibleCopy(label = label, value = value)
    }
    if (metric == QuickGlanceMetric.HEALTH && value.contains(HEALTH_STATUS_SEPARATOR)) {
        val score = value.substringBefore(HEALTH_STATUS_SEPARATOR).trim()
        val status = value.substringAfter(HEALTH_STATUS_SEPARATOR).trim()
        return QuickGlanceVisibleCopy(label = "$score $compactLabel", value = status)
    }
    return QuickGlanceVisibleCopy(label = compactLabel, value = value)
}

private fun fitWidgetTextToWidth(
    text: String,
    preferredFontSizeSp: Int,
    minimumFontSizeSp: Int,
    fontScale: Float,
    widthDp: Float,
): FittedWidgetText {
    for (fontSizeSp in preferredFontSizeSp downTo minimumFontSizeSp) {
        if (estimateWidgetTextWidthDp(text, fontSizeSp, fontScale) <= widthDp) {
            return FittedWidgetText(text = text, fontSizeSp = fontSizeSp)
        }
    }
    return FittedWidgetText(
        text = text.ellipsizeToWidgetWidth(minimumFontSizeSp, fontScale, widthDp),
        fontSizeSp = minimumFontSizeSp,
    )
}

private fun String.ellipsizeToWidgetWidth(
    fontSizeSp: Int,
    fontScale: Float,
    widthDp: Float,
): String {
    if (estimateWidgetTextWidthDp(this, fontSizeSp, fontScale) <= widthDp) return this
    var prefix = dropLast(1)
    while (prefix.isNotEmpty()) {
        val candidate = "$prefix…"
        if (estimateWidgetTextWidthDp(candidate, fontSizeSp, fontScale) <= widthDp) return candidate
        prefix = prefix.dropLast(1)
    }
    return "…"
}

internal fun estimateWidgetTextWidthDp(
    text: String,
    fontSizeSp: Int,
    fontScale: Float,
): Float =
    text.sumOf { character -> character.conservativeEmWidth().toDouble() }.toFloat() *
        fontSizeSp *
        fontScale *
        WIDGET_TEXT_WIDTH_SAFETY_FACTOR

private fun Char.conservativeEmWidth(): Float =
    when {
        isDigit() -> 0.62f
        isUpperCase() -> 0.70f
        isLowerCase() -> 0.62f
        isWhitespace() -> 0.38f
        this == '%' -> 0.82f
        this == '°' -> 0.55f
        this == '·' -> 0.50f
        else -> 0.55f
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
                            compactLabel = context.getString(R.string.widget_health_compact_label),
                        ),
                    QuickGlanceMetric.BATTERY to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_battery_label),
                            value = batteryValue,
                            compactLabel = context.getString(R.string.widget_battery_label),
                        ),
                    QuickGlanceMetric.STORAGE to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_free_storage_label),
                            value = storageValue,
                            compactLabel = context.getString(R.string.widget_free_storage_compact_label),
                        ),
                    QuickGlanceMetric.TEMPERATURE to
                        QuickGlanceCellValue(
                            label = context.getString(R.string.widget_temperature_label),
                            value = temperatureValue,
                            compactLabel = context.getString(R.string.widget_temperature_compact_label),
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
    context: Context,
    model: QuickGlanceCellModel,
    presentation: QuickGlancePresentation,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .clickable(actionStartActivity(widgetNavigationIntent(context, model.metric.route)))
                .semantics {
                    contentDescription = model.accessibilityLabel
                    testTag = "quick_glance_${model.metric.name.lowercase()}"
                }.padding(presentation.cellPaddingDp.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = model.displayValue,
            maxLines = presentation.valueMaxLines,
            style =
                TextStyle(
                    fontSize = model.valueFontSp.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
        )
        Text(
            text = model.displayLabel,
            maxLines = presentation.labelMaxLines,
            style =
                TextStyle(
                    fontSize = model.labelFontSp.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }
}

class QuickGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickGlanceWidget()
}

private const val QUICK_GLANCE_ROW_COUNT = 2f
private const val QUICK_GLANCE_COLUMN_COUNT = 2f
private const val QUICK_GLANCE_OUTER_PADDING_DP = 12
private const val QUICK_GLANCE_CELL_PADDING_DP = 4
private const val QUICK_GLANCE_MINIMUM_FONT_SP = 5
private const val WIDGET_TEXT_WIDTH_SAFETY_FACTOR = 1.1f
private const val HEALTH_STATUS_SEPARATOR = "·"
