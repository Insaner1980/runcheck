package com.runcheck.widget

import android.content.Context
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
        if (!WidgetDataProvider.isProUnlocked(context)) {
            provideContent { WidgetLockedContent(context, R.string.widget_battery_name) }
            return
        }
        val snapshot = WidgetDataProvider.loadBatterySnapshot(context)
        if (snapshot == null) {
            provideContent { WidgetEmptyContent(context) }
            return
        }

        val levelText = context.getString(R.string.widget_percent_value, snapshot.level)
        val tempText = context.getString(R.string.widget_temperature_value, snapshot.temperatureC)
        val currentDisplay =
            snapshot.currentMa?.let {
                context.getString(R.string.widget_current_value, it)
            }

        provideContent {
            GlanceTheme {
                Column(
                    modifier = widgetContainerModifier(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = levelText,
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
                                style =
                                    TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant,
                                    ),
                            )
                            currentDisplay?.let {
                                Text(
                                    text = it,
                                    style =
                                        TextStyle(
                                            fontSize = 12.sp,
                                            color = GlanceTheme.colors.onSurfaceVariant,
                                        ),
                                )
                            }
                        }
                    }
                    val size = androidx.glance.LocalSize.current
                    if (size.width >= MEDIUM.width) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = context.getString(R.string.widget_battery_name),
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
}

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()
}
