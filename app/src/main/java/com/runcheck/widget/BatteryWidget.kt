package com.runcheck.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.runcheck.R
import com.runcheck.ui.navigation.Screen

internal enum class WidgetLayout {
    COMPACT,
    STANDARD,
    EXPANDED,
}

internal data class BatteryWidgetPresentation(
    val layout: WidgetLayout,
    val outerPaddingDp: Int,
    val valueFontSp: Int,
    val detailFontSp: Int,
    val titleFontSp: Int,
    val showCurrent: Boolean,
    val showTitle: Boolean,
    val valueMaxLines: Int = 1,
    val detailMaxLines: Int = 1,
) {
    fun requiredContentHeightDp(fontScale: Float): Float {
        val valueLineHeight = valueFontSp * fontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER
        val detailLines = if (showCurrent) 2 else 1
        val detailHeight = detailFontSp * fontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER * detailLines
        val titleHeight =
            if (showTitle) {
                BATTERY_TITLE_SPACING_DP +
                    titleFontSp * fontScale * WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER
            } else {
                0f
            }
        return maxOf(valueLineHeight, detailHeight) + titleHeight
    }

    fun requiredTotalHeightDp(fontScale: Float): Float =
        outerPaddingDp * 2f + requiredContentHeightDp(fontScale)
}

internal fun batteryWidgetLayoutFor(size: DpSize): WidgetLayout =
    when {
        size.width >= 250.dp && size.height >= 100.dp -> WidgetLayout.EXPANDED
        size.width >= 180.dp && size.height >= 72.dp -> WidgetLayout.STANDARD
        else -> WidgetLayout.COMPACT
    }

internal fun batteryWidgetPresentationFor(
    size: DpSize,
    fontScale: Float,
): BatteryWidgetPresentation {
    val layout = batteryWidgetLayoutFor(size)
    val typography =
        when {
            fontScale >= 1.75f -> Triple(14, 8, 8)
            fontScale >= 1.2f -> Triple(20, 10, 9)
            else -> Triple(28, 12, 11)
        }
    return when (layout) {
        WidgetLayout.COMPACT ->
            BatteryWidgetPresentation(
                layout = layout,
                outerPaddingDp = 8,
                valueFontSp = typography.first.coerceAtMost(24),
                detailFontSp = typography.second.coerceAtMost(10),
                titleFontSp = typography.third,
                showCurrent = false,
                showTitle = false,
            )
        WidgetLayout.STANDARD ->
            BatteryWidgetPresentation(
                layout = layout,
                outerPaddingDp = 8,
                valueFontSp = typography.first,
                detailFontSp = typography.second,
                titleFontSp = typography.third,
                showCurrent = fontScale < 1.75f,
                showTitle = false,
            )
        WidgetLayout.EXPANDED ->
            BatteryWidgetPresentation(
                layout = layout,
                outerPaddingDp = 12,
                valueFontSp = typography.first,
                detailFontSp = typography.second,
                titleFontSp = typography.third,
                showCurrent = true,
                showTitle = true,
            )
    }
}

internal data class BatteryWidgetTextModel(
    val level: String,
    val temperature: String,
    val current: String?,
    val title: String?,
)

internal fun batteryWidgetTextModel(
    level: String,
    temperature: String,
    current: String?,
    title: String,
    presentation: BatteryWidgetPresentation,
): BatteryWidgetTextModel =
    BatteryWidgetTextModel(
        level = level,
        temperature = temperature,
        current = current.takeIf { presentation.showCurrent },
        title = title.takeIf { presentation.showTitle },
    )

class BatteryWidget : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(110.dp, 72.dp)
        private val MEDIUM = DpSize(180.dp, 72.dp)
        private val LARGE = DpSize(250.dp, 100.dp)
    }

    override val sizeMode =
        SizeMode.Responsive(
            setOf(SMALL, MEDIUM, LARGE),
        )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            val widgetState by
                WidgetDataProvider
                    .observeBatteryWidgetState(context)
                    .collectAsState(initial = WidgetRenderState.Loading)

            when (val state = widgetState) {
                WidgetRenderState.Empty -> WidgetEmptyContent(context, Screen.Battery.route)
                WidgetRenderState.Loading -> WidgetLoadingContent(context, Screen.Battery.route)
                WidgetRenderState.Locked -> WidgetLockedContent(context, R.string.widget_battery_name)
                WidgetRenderState.Stale -> WidgetStaleContent(context, Screen.Battery.route)
                WidgetRenderState.Unavailable -> WidgetUnavailableContent(context, Screen.Battery.route)
                is WidgetRenderState.Content -> BatteryWidgetContent(context, state.snapshot)
            }
        }
    }

    @Composable
    private fun BatteryWidgetContent(
        context: Context,
        snapshot: BatteryWidgetSnapshot,
    ) {
        val fontScale = context.resources.configuration.fontScale
        val presentation = batteryWidgetPresentationFor(androidx.glance.LocalSize.current, fontScale)
        val text =
            batteryWidgetTextModel(
                level = context.getString(R.string.widget_percent_value, snapshot.level),
                temperature = context.getString(R.string.widget_temperature_value, snapshot.temperatureC),
                current = snapshot.currentMa?.let { context.getString(R.string.widget_current_value, it) },
                title = context.getString(R.string.widget_battery_name),
                presentation = presentation,
            )

        RuncheckWidgetTheme {
            Column(
                modifier =
                    widgetContainerModifier(
                        context = context,
                        route = Screen.Battery.route,
                        padding = presentation.outerPaddingDp.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = text.level,
                        maxLines = presentation.valueMaxLines,
                        style =
                            TextStyle(
                                fontSize = presentation.valueFontSp.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = text.temperature,
                            maxLines = presentation.detailMaxLines,
                            style =
                                TextStyle(
                                    fontSize = presentation.detailFontSp.sp,
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                ),
                        )
                        text.current?.let {
                            Text(
                                text = it,
                                maxLines = presentation.detailMaxLines,
                                style =
                                    TextStyle(
                                        fontSize = presentation.detailFontSp.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                    ),
                            )
                        }
                    }
                }
                if (presentation.showTitle) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = requireNotNull(text.title),
                        maxLines = 1,
                        style =
                            TextStyle(
                                fontSize = presentation.titleFontSp.sp,
                                color = GlanceTheme.colors.onSurfaceVariant,
                            ),
                    )
                }
            }
        }
    }
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()
}

private const val BATTERY_TITLE_SPACING_DP = 4f
