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

internal fun batteryWidgetLayoutFor(size: DpSize): WidgetLayout =
    when {
        size.width >= 250.dp && size.height >= 100.dp -> WidgetLayout.EXPANDED
        size.width >= 180.dp && size.height >= 60.dp -> WidgetLayout.STANDARD
        else -> WidgetLayout.COMPACT
    }

class BatteryWidget : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(110.dp, 40.dp)
        private val MEDIUM = DpSize(180.dp, 60.dp)
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
        val levelText = context.getString(R.string.widget_percent_value, snapshot.level)
        val tempText = context.getString(R.string.widget_temperature_value, snapshot.temperatureC)
        val currentDisplay =
            snapshot.currentMa?.let {
                context.getString(R.string.widget_current_value, it)
            }

        RuncheckWidgetTheme {
            Column(
                modifier = widgetContainerModifier(context, Screen.Battery.route),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = levelText,
                        maxLines = 1,
                        style =
                            TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface,
                            ),
                    )
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Column {
                        Text(
                            text = tempText,
                            maxLines = 1,
                            style =
                                TextStyle(
                                    fontSize = 12.sp,
                                    color = GlanceTheme.colors.onSurfaceVariant,
                                ),
                        )
                        currentDisplay?.let {
                            Text(
                                text = it,
                                maxLines = 1,
                                style =
                                    TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                    ),
                            )
                        }
                    }
                }
                val layout = batteryWidgetLayoutFor(androidx.glance.LocalSize.current)
                if (layout != WidgetLayout.COMPACT) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = context.getString(R.string.widget_battery_name),
                        maxLines = 1,
                        style =
                            TextStyle(
                                fontSize = 11.sp,
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
