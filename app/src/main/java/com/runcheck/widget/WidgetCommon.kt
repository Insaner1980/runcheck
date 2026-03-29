package com.runcheck.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.runcheck.MainActivity
import com.runcheck.R

@Composable
internal fun widgetContainerModifier(): GlanceModifier =
    GlanceModifier
        .fillMaxSize()
        .padding(12.dp)
        .cornerRadius(16.dp)
        .background(GlanceTheme.colors.widgetBackground)
        .clickable(actionStartActivity<MainActivity>())

@Composable
internal fun WidgetLockedContent(
    context: Context,
    widgetNameResId: Int,
) {
    GlanceTheme {
        Column(
            modifier = widgetContainerModifier(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(widgetNameResId),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Text(
                text = context.getString(R.string.settings_upgrade_pro),
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }
}

@Composable
internal fun WidgetEmptyContent(context: Context) {
    GlanceTheme {
        Column(
            modifier = widgetContainerModifier(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(R.string.widget_no_data_title),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Text(
                text = context.getString(R.string.widget_no_data_message),
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }
}
