package com.runcheck.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.runcheck.MainActivity
import com.runcheck.R
import com.runcheck.domain.model.HealthStatus
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.ui.navigation.Screen
import com.runcheck.ui.theme.AccentAmber
import com.runcheck.ui.theme.AccentBlue
import com.runcheck.ui.theme.AccentRed
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.BgCard
import com.runcheck.ui.theme.BgIconCircle
import com.runcheck.ui.theme.BgPage
import com.runcheck.ui.theme.LightBackground
import com.runcheck.ui.theme.LightError
import com.runcheck.ui.theme.LightOnSurface
import com.runcheck.ui.theme.LightOnSurfaceVariant
import com.runcheck.ui.theme.LightOutline
import com.runcheck.ui.theme.LightOutlineVariant
import com.runcheck.ui.theme.LightPrimary
import com.runcheck.ui.theme.LightSecondary
import com.runcheck.ui.theme.LightSurface
import com.runcheck.ui.theme.LightSurfaceContainer
import com.runcheck.ui.theme.LightSurfaceContainerHigh
import com.runcheck.ui.theme.LightSurfaceContainerHighest
import com.runcheck.ui.theme.StatusCritical
import com.runcheck.ui.theme.StatusFair
import com.runcheck.ui.theme.StatusHealthy
import com.runcheck.ui.theme.StatusPoor
import com.runcheck.ui.theme.TextMuted
import com.runcheck.ui.theme.TextPrimary
import com.runcheck.ui.theme.TextSecondary
import com.runcheck.ui.theme.WidgetStatusCriticalNight
import com.runcheck.ui.theme.WidgetStatusPoorNight
import androidx.glance.unit.ColorProvider as GlanceColorProvider

internal val RuncheckWidgetColors: ColorProviders =
    colorProviders(
        primary = ColorProvider(day = LightPrimary, night = AccentBlue),
        onPrimary = ColorProvider(day = LightSurface, night = BgPage),
        primaryContainer = ColorProvider(day = LightSurfaceContainerHigh, night = BgIconCircle),
        onPrimaryContainer = ColorProvider(day = LightOnSurface, night = TextPrimary),
        secondary = ColorProvider(day = LightSecondary, night = AccentTeal),
        onSecondary = ColorProvider(day = LightSurface, night = BgPage),
        secondaryContainer = ColorProvider(day = LightSurfaceContainerHighest, night = BgIconCircle),
        onSecondaryContainer = ColorProvider(day = LightOnSurface, night = TextPrimary),
        tertiary = ColorProvider(day = LightPrimary, night = AccentBlue),
        onTertiary = ColorProvider(day = LightSurface, night = BgPage),
        tertiaryContainer = ColorProvider(day = LightSurfaceContainer, night = BgCard),
        onTertiaryContainer = ColorProvider(day = LightOnSurface, night = TextPrimary),
        error = ColorProvider(day = LightError, night = AccentRed),
        errorContainer = ColorProvider(day = LightError, night = AccentRed),
        onError = ColorProvider(day = LightSurface, night = BgPage),
        onErrorContainer = ColorProvider(day = LightSurface, night = BgPage),
        background = ColorProvider(day = LightBackground, night = BgPage),
        onBackground = ColorProvider(day = LightOnSurface, night = TextPrimary),
        surface = ColorProvider(day = LightSurface, night = BgPage),
        onSurface = ColorProvider(day = LightOnSurface, night = TextPrimary),
        surfaceVariant = ColorProvider(day = LightSurfaceContainerHighest, night = BgIconCircle),
        onSurfaceVariant = ColorProvider(day = LightOnSurfaceVariant, night = TextSecondary),
        outline = ColorProvider(day = LightOutline, night = TextMuted),
        inverseOnSurface = ColorProvider(day = LightSurface, night = BgPage),
        inverseSurface = ColorProvider(day = LightOnSurface, night = TextPrimary),
        inversePrimary = ColorProvider(day = LightPrimary, night = AccentBlue),
        widgetBackground = ColorProvider(day = LightSurfaceContainer, night = BgCard),
    )

internal data class WidgetStatusTone(
    val name: String,
    val day: Color,
    val night: Color,
) {
    val provider: GlanceColorProvider = ColorProvider(day = day, night = night)
}

internal object RuncheckWidgetStatusPalette {
    val healthy = WidgetStatusTone("healthy", StatusHealthy, AccentTeal)
    val fair = WidgetStatusTone("fair", StatusFair, AccentAmber)
    val poor = WidgetStatusTone("poor", StatusPoor, WidgetStatusPoorNight)
    val critical = WidgetStatusTone("critical", StatusCritical, WidgetStatusCriticalNight)
    val all = listOf(healthy, fair, poor, critical)

    fun forHealthStatus(status: HealthStatus): GlanceColorProvider =
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
