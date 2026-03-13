package com.devicepulse.widget

import android.content.Context
import android.os.BatteryManager
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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class BatteryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (!ProStatusCache.isPro(context)) {
            provideLockedContent(context)
            return
        }
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val normalizedLevel = level.takeUnless { it == Int.MIN_VALUE } ?: 0
        val tempRaw = batteryManager.getIntProperty(4) // BATTERY_PROPERTY_TEMPERATURE isn't public
        val tempC = if (tempRaw > 0) tempRaw / 10f else null
        val currentMa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val levelText = context.getString(R.string.widget_percent_value, normalizedLevel)
        val currentDisplay = if (currentMa != 0 && currentMa != Int.MIN_VALUE) {
            context.getString(R.string.widget_current_value, currentMa / 1000)
        } else null

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.widgetBackground),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = levelText,
                            style = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlanceTheme.colors.onSurface
                            )
                        )
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        Column {
                            tempC?.let {
                                Text(
                                    text = context.getString(R.string.widget_temperature_value, it),
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = GlanceTheme.colors.onSurfaceVariant
                                    )
                                )
                            }
                            currentDisplay?.let {
                                Text(
                                    text = it,
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
                        text = context.getString(R.string.widget_battery_name),
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurface
                        )
                    )
                    Text(
                        text = context.getString(R.string.widget_pro_required),
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

class BatteryWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BatteryWidget()
}
