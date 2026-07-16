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
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.ui.common.healthStatusLabelRes
import com.runcheck.ui.theme.RuncheckStatusColors
import com.runcheck.ui.theme.forHealthStatus

class HealthWidget : GlanceAppWidget() {
    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val MEDIUM = DpSize(180.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 150.dp)
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
                    .observeHealthWidgetState(context)
                    .collectAsState(initial = WidgetRenderState.Locked)

            when (val state = widgetState) {
                WidgetRenderState.Empty -> WidgetEmptyContent(context)
                WidgetRenderState.Locked -> WidgetLockedContent(context, R.string.widget_health_name)
                WidgetRenderState.Stale -> WidgetStaleContent(context)
                is WidgetRenderState.Content -> HealthWidgetContent(context, state.snapshot)
            }
        }
    }

    @Composable
    private fun HealthWidgetContent(
        context: Context,
        snapshot: HealthWidgetSnapshot,
    ) {
        val healthScoreLabel = context.getString(R.string.widget_health_score_label)
        val status = HealthScore.statusFromScore(snapshot.overallScore)
        val statusLabel = context.getString(healthStatusLabelRes(status))
        val scoreLabel = context.getString(R.string.widget_health_score_with_status, healthScoreLabel, statusLabel)
        val batteryLabel = context.getString(R.string.widget_battery_short_label)
        val batteryValue = context.getString(R.string.widget_percent_value, snapshot.batteryLevel)
        val statusColor = RuncheckStatusColors.forHealthStatus(status)
        val statusColorProvider = ColorProvider(statusColor, statusColor)

        GlanceTheme {
            val size = LocalSize.current
            Column(
                modifier = widgetContainerModifier(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = snapshot.overallScore.toString(),
                    style =
                        TextStyle(
                            fontSize = if (size.width >= LARGE.width) 48.sp else 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColorProvider,
                        ),
                )
                Text(
                    text = scoreLabel,
                    style =
                        TextStyle(
                            fontSize = 12.sp,
                            color = statusColorProvider,
                        ),
                )
                Spacer(modifier = GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MiniIndicator(label = batteryLabel, value = batteryValue)
                }
            }
        }
    }
}

@Composable
private fun MiniIndicator(
    label: String,
    value: String,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = GlanceModifier.padding(horizontal = 4.dp),
    ) {
        Text(
            text = value,
            style =
                TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GlanceTheme.colors.onSurface,
                ),
        )
        Text(
            text = label,
            style =
                TextStyle(
                    fontSize = 10.sp,
                    color = GlanceTheme.colors.onSurfaceVariant,
                ),
        )
    }
}

class HealthWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HealthWidget()
}
