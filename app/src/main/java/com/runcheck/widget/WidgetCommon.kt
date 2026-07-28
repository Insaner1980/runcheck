package com.runcheck.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.runcheck.MainActivity
import com.runcheck.R
import com.runcheck.domain.model.HealthStatus
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.Screen

internal val RuncheckWidgetColors: ColorProviders =
    colorProviders(
        primary = ColorProvider(R.color.widget_primary),
        onPrimary = ColorProvider(R.color.widget_on_primary),
        primaryContainer = ColorProvider(R.color.widget_primary_container),
        onPrimaryContainer = ColorProvider(R.color.widget_on_primary_container),
        secondary = ColorProvider(R.color.widget_secondary),
        onSecondary = ColorProvider(R.color.widget_on_secondary),
        secondaryContainer = ColorProvider(R.color.widget_secondary_container),
        onSecondaryContainer = ColorProvider(R.color.widget_on_secondary_container),
        tertiary = ColorProvider(R.color.widget_tertiary),
        onTertiary = ColorProvider(R.color.widget_on_tertiary),
        tertiaryContainer = ColorProvider(R.color.widget_tertiary_container),
        onTertiaryContainer = ColorProvider(R.color.widget_on_tertiary_container),
        error = ColorProvider(R.color.widget_error),
        errorContainer = ColorProvider(R.color.widget_error),
        onError = ColorProvider(R.color.widget_on_error),
        onErrorContainer = ColorProvider(R.color.widget_on_error),
        background = ColorProvider(R.color.widget_page_background),
        onBackground = ColorProvider(R.color.widget_on_surface),
        surface = ColorProvider(R.color.widget_surface),
        onSurface = ColorProvider(R.color.widget_on_surface),
        surfaceVariant = ColorProvider(R.color.widget_surface_variant),
        onSurfaceVariant = ColorProvider(R.color.widget_on_surface_variant),
        outline = ColorProvider(R.color.widget_outline),
        inverseOnSurface = ColorProvider(R.color.widget_surface),
        inverseSurface = ColorProvider(R.color.widget_on_surface),
        inversePrimary = ColorProvider(R.color.widget_primary),
        widgetBackground = ColorProvider(R.color.widget_background),
    )

internal data class WidgetStatusTone(
    val name: String,
    val colorRes: Int,
) {
    val provider: ColorProvider = ColorProvider(colorRes)
}

internal object RuncheckWidgetStatusPalette {
    val healthy = WidgetStatusTone("healthy", R.color.widget_status_healthy)
    val fair = WidgetStatusTone("fair", R.color.widget_status_fair)
    val poor = WidgetStatusTone("poor", R.color.widget_status_poor)
    val critical = WidgetStatusTone("critical", R.color.widget_status_critical)
    val all = listOf(healthy, fair, poor, critical)

    fun forHealthStatus(status: HealthStatus): ColorProvider =
        when (status) {
            HealthStatus.HEALTHY -> healthy.provider
            HealthStatus.FAIR -> fair.provider
            HealthStatus.POOR -> poor.provider
            HealthStatus.CRITICAL -> critical.provider
        }
}

internal const val WIDGET_TEXT_LINE_HEIGHT_MULTIPLIER = 1.2f

@Composable
internal fun RuncheckWidgetTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = RuncheckWidgetColors, content = content)
}

internal fun widgetNavigationIntent(
    context: Context,
    route: String,
): Intent {
    require(Screen.isDirectRoute(route)) { "Widget route must be directly reachable: $route" }
    return Intent(context, MainActivity::class.java).apply {
        action = "${context.packageName}.widget.OPEN.$route"
        putExtra(NotificationHelper.EXTRA_NAVIGATE_TO, route)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}

@Composable
internal fun widgetSurfaceModifier(padding: Dp = 12.dp): GlanceModifier =
    GlanceModifier
        .fillMaxSize()
        .padding(padding)
        .cornerRadius(16.dp)
        .background(GlanceTheme.colors.widgetBackground)

@Composable
internal fun widgetContainerModifier(
    context: Context,
    route: String,
    padding: Dp = 12.dp,
): GlanceModifier =
    widgetSurfaceModifier(padding)
        .clickable(actionStartActivity(widgetNavigationIntent(context, route)))

@Composable
internal fun WidgetLoadingContent(
    context: Context,
    route: String,
) {
    WidgetMessageContent(
        context = context,
        route = route,
        titleResId = R.string.widget_loading_title,
        messageResId = R.string.widget_loading_message,
    )
}

@Composable
internal fun WidgetLockedContent(
    context: Context,
    widgetNameResId: Int,
) {
    WidgetMessageContent(
        context = context,
        route = Screen.ProUpgrade.route,
        titleResId = widgetNameResId,
        messageResId = R.string.settings_upgrade_pro,
    )
}

@Composable
internal fun WidgetEmptyContent(
    context: Context,
    route: String,
) {
    WidgetMessageContent(
        context = context,
        route = route,
        titleResId = R.string.widget_no_data_title,
        messageResId = R.string.widget_no_data_message,
    )
}

@Composable
internal fun WidgetStaleContent(
    context: Context,
    route: String,
) {
    WidgetMessageContent(
        context = context,
        route = route,
        titleResId = R.string.widget_stale_data_title,
        messageResId = R.string.widget_stale_data_message,
    )
}

@Composable
internal fun WidgetUnavailableContent(
    context: Context,
    route: String,
) {
    WidgetMessageContent(
        context = context,
        route = route,
        titleResId = R.string.widget_unavailable_title,
        messageResId = R.string.widget_unavailable_message,
    )
}

@Composable
private fun WidgetMessageContent(
    context: Context,
    route: String,
    titleResId: Int,
    messageResId: Int,
) {
    RuncheckWidgetTheme {
        Column(
            modifier = widgetContainerModifier(context, route),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = context.getString(titleResId),
                maxLines = 1,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlanceTheme.colors.onSurface,
                    ),
            )
            Text(
                text = context.getString(messageResId),
                maxLines = 2,
                style =
                    TextStyle(
                        fontSize = 12.sp,
                        color = GlanceTheme.colors.onSurfaceVariant,
                    ),
            )
        }
    }
}
