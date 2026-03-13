package com.devicepulse.widget

import android.content.Context
import com.devicepulse.R
import com.devicepulse.data.billing.ProStatusCache
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HealthWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!ProStatusCache.isPro(context)) {
            provideLockedContent(context)
            return
        }
        val snapshot = WidgetDataProvider.loadHealthSnapshot(context)
        if (snapshot == null) {
            provideEmptyContent(context)
            return
        }
        val healthScoreLabel = context.getString(R.string.widget_health_score_label)
        val batteryLabel = context.getString(R.string.widget_battery_short_label)
        val batteryValue = context.getString(R.string.widget_percent_value, snapshot.batteryLevel)

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = snapshot.overallScore.toString(),
                        style = TextStyle(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = healthScoreLabel,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MiniIndicator(label = batteryLabel, value = batteryValue)
                    }
                }
            }
        }
    }

    private suspend fun provideLockedContent(context: Context) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = context.getString(R.string.widget_health_name),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = context.getString(R.string.settings_upgrade_pro),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }

    private suspend fun provideEmptyContent(context: Context) {
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = context.getString(R.string.widget_no_data_title),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = context.getString(R.string.widget_no_data_message),
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MiniIndicator(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurface
            )
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 10.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

class HealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthWidget()
}
