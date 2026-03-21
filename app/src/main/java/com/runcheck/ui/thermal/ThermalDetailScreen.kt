package com.runcheck.ui.thermal

import com.runcheck.ui.ads.DetailScreenAdBanner
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.TemperatureUnit
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.ThrottlingEvent
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.formatTemperatureValue
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.temperatureUnitRes
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.HeatStrip
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.components.info.InfoBottomSheet
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.theme.iconCircleColor
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericHeroValueTextStyle
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.statusColors

@Composable
fun ThermalDetailScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ThermalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startObserving()
                Lifecycle.Event.ON_STOP -> viewModel.stopObserving()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startObserving()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopObserving()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.thermal_title),
            onBack = onBack
        )
        when (val state = uiState) {
            is ThermalUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_loading); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ThermalUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    ) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is ThermalUiState.Success -> {
                ThermalContent(
                    state = state,
                    onRefresh = { viewModel.refresh() },
                    onUpgradeToPro = onUpgradeToPro,
                    onDismissInfoCard = { viewModel.dismissInfoCard(it) }
                )
            }
        }
    }
}

@Composable
private fun ThermalContent(
    state: ThermalUiState.Success,
    onRefresh: () -> Unit,
    onUpgradeToPro: () -> Unit,
    onDismissInfoCard: (String) -> Unit
) {
    var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val thermal = state.thermalState

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm)) }

            // Hero card: thermometer + big temperature + status
            item {
                ThermalHeroCard(
                    thermal = thermal,
                    temperatureUnit = state.temperatureUnit,
                    sessionMinTemp = state.sessionMinTemp,
                    sessionMaxTemp = state.sessionMaxTemp
                )
            }

            // HeatStrip
            item {
                HeatStrip(
                    temperatureC = thermal.batteryTempC,
                    temperatureUnit = state.temperatureUnit
                )
            }

            // Info cards
            if (ThermalInfoCards.THROTTLING_EXPLAINER !in state.dismissedInfoCards) {
                item {
                    InfoCard(
                        id = ThermalInfoCards.THROTTLING_EXPLAINER,
                        headline = stringResource(R.string.info_card_thermal_throttling_headline),
                        body = stringResource(R.string.info_card_thermal_throttling_body),
                        onDismiss = { onDismissInfoCard(it) }
                    )
                }
            }

            if (thermal.batteryTempC > 35f &&
                ThermalInfoCards.HEAT_BATTERY_LOOP !in state.dismissedInfoCards
            ) {
                item {
                    InfoCard(
                        id = ThermalInfoCards.HEAT_BATTERY_LOOP,
                        headline = stringResource(R.string.info_card_heat_battery_headline),
                        body = stringResource(R.string.info_card_heat_battery_body),
                        onDismiss = { onDismissInfoCard(it) }
                    )
                }
            }

            // Metrics grid
            item {
                ThermalMetricsCard(
                    thermal = thermal,
                    temperatureUnit = state.temperatureUnit,
                    onInfoClick = { key -> activeInfoSheet = key }
                )
            }

            // Throttling section
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                SectionHeader(stringResource(R.string.thermal_throttling_log))
            }

            if (state.isPro) {
                if (state.throttlingEvents.isEmpty()) {
                    item {
                        ThrottlingEmptyState()
                    }
                } else {
                    items(
                        items = state.throttlingEvents,
                        key = { event -> event.id.takeIf { it != 0L } ?: event.timestamp }
                    ) { event ->
                        ThrottlingEventItem(
                            event = event,
                            temperatureUnit = state.temperatureUnit
                        )
                    }
                }
            } else {
                item {
                    ProFeatureCalloutCard(
                        message = stringResource(R.string.pro_feature_thermal_log_message),
                        actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                        onAction = onUpgradeToPro
                    )
                }
            }

            item { DetailScreenAdBanner() }
            item { Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl)) }
        }
    }

    activeInfoSheet?.let { key ->
        val content = when (key) {
            "cpuTemp" -> ThermalInfoContent.cpuTemp
            "thermalHeadroom" -> ThermalInfoContent.thermalHeadroom
            "thermalStatus" -> ThermalInfoContent.thermalStatus
            "throttling" -> ThermalInfoContent.throttling
            else -> null
        }
        content?.let {
            InfoBottomSheet(
                content = it,
                onDismiss = { activeInfoSheet = null }
            )
        }
    }
}

// ── Hero card ────────────────────────────────────────────────────────────────────

@Composable
private fun ThermalHeroCard(
    thermal: ThermalState,
    temperatureUnit: TemperatureUnit,
    sessionMinTemp: Float? = null,
    sessionMaxTemp: Float? = null
) {
    val tempColor = statusColorForTemperature(thermal.batteryTempC)
    val bandLabel = temperatureBandLabel(thermal.batteryTempC)

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.lg, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionHeader(stringResource(R.string.thermal_battery_temp))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Thermometer visual
                ThermometerIcon(
                    temperatureC = thermal.batteryTempC,
                    temperatureUnit = temperatureUnit,
                    color = tempColor,
                    modifier = Modifier.size(width = 40.dp, height = 120.dp)
                )

                Spacer(modifier = Modifier.width(MaterialTheme.spacing.lg))

                // Temperature number + status
                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = formatTemperatureValue(thermal.batteryTempC, temperatureUnit),
                            style = MaterialTheme.numericHeroValueTextStyle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(temperatureUnitRes(temperatureUnit)),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = MaterialTheme.numericFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 2.dp, bottom = 10.dp)
                        )
                    }
                    Text(
                        text = bandLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = tempColor
                    )
                    if (sessionMinTemp != null && sessionMaxTemp != null &&
                        sessionMinTemp != sessionMaxTemp
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = statusColorForTemperature(sessionMinTemp))) {
                                    append(
                                        stringResource(
                                            R.string.value_direction_down,
                                            formatTemperature(sessionMinTemp, temperatureUnit)
                                        )
                                    )
                                }
                                append(" · ")
                                withStyle(SpanStyle(color = statusColorForTemperature(sessionMaxTemp))) {
                                    append(
                                        stringResource(
                                            R.string.value_direction_up,
                                            formatTemperature(sessionMaxTemp, temperatureUnit)
                                        )
                                    )
                                }
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }
    }
}

// ── Thermometer icon (Canvas) ────────────────────────────────────────────────────

@Composable
private fun ThermometerIcon(
    temperatureC: Float,
    temperatureUnit: TemperatureUnit,
    color: Color,
    modifier: Modifier = Modifier,
    minTemp: Float = 15f,
    maxTemp: Float = 50f
) {
    val normalizedTemp = ((temperatureC - minTemp) / (maxTemp - minTemp)).coerceIn(0f, 1f)
    val reducedMotion = MaterialTheme.reducedMotion

    val animatedFill by animateFloatAsState(
        targetValue = normalizedTemp,
        animationSpec = if (reducedMotion) tween(0) else tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "thermFill"
    )

    val trackColor = MaterialTheme.iconCircleColor
    val a11yDesc = stringResource(
        R.string.a11y_heat_strip,
        formatTemperature(temperatureC, temperatureUnit),
        temperatureBandLabel(temperatureC)
    )

    Canvas(
        modifier = modifier.semantics { contentDescription = a11yDesc }
    ) {
        val w = size.width
        val h = size.height

        // Bulb at the bottom
        val bulbRadius = w * 0.42f
        val bulbCenterY = h - bulbRadius - 2.dp.toPx()

        // Stem dimensions
        val stemWidth = w * 0.30f
        val stemLeft = (w - stemWidth) / 2f
        val stemTop = 4.dp.toPx()
        val stemBottom = bulbCenterY - bulbRadius * 0.5f
        val stemHeight = stemBottom - stemTop
        val stemCorner = stemWidth / 2f

        // Track (background) — stem
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(stemLeft, stemTop),
            size = Size(stemWidth, stemHeight),
            cornerRadius = CornerRadius(stemCorner, stemCorner)
        )

        // Track (background) — bulb
        drawCircle(
            color = trackColor,
            radius = bulbRadius,
            center = Offset(w / 2f, bulbCenterY)
        )

        // Fill — bulb (always filled with status color)
        drawCircle(
            color = color,
            radius = bulbRadius * 0.75f,
            center = Offset(w / 2f, bulbCenterY)
        )

        // Fill — stem (from bottom up based on temperature)
        val fillHeight = stemHeight * animatedFill
        val fillTop = stemTop + stemHeight - fillHeight
        if (fillHeight > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(stemLeft + stemWidth * 0.15f, fillTop),
                size = Size(stemWidth * 0.70f, stemBottom - fillTop),
                cornerRadius = CornerRadius(stemWidth * 0.35f, stemWidth * 0.35f)
            )
        }

        // Stem outline
        drawRoundRect(
            color = color.copy(alpha = 0.4f),
            topLeft = Offset(stemLeft, stemTop),
            size = Size(stemWidth, stemHeight),
            cornerRadius = CornerRadius(stemCorner, stemCorner),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Bulb outline
        drawCircle(
            color = color.copy(alpha = 0.4f),
            radius = bulbRadius,
            center = Offset(w / 2f, bulbCenterY),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Tick marks on the stem
        val tickCount = 4
        val tickStartX = stemLeft + stemWidth + 3.dp.toPx()
        val tickEndX = tickStartX + 6.dp.toPx()
        for (i in 1 until tickCount) {
            val tickY = stemTop + stemHeight * (1f - i.toFloat() / tickCount)
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(tickStartX, tickY),
                end = Offset(tickEndX, tickY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

// ── Metrics grid card ────────────────────────────────────────────────────────────

@Composable
private fun ThermalMetricsCard(
    thermal: ThermalState,
    temperatureUnit: TemperatureUnit,
    onInfoClick: (String) -> Unit = {}
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            // Row 1: CPU Temperature + Thermal Headroom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                MetricPill(
                    label = stringResource(R.string.thermal_cpu_temp),
                    value = thermal.cpuTempC?.let {
                        formatTemperature(it, temperatureUnit)
                    } ?: stringResource(R.string.thermal_cpu_unavailable),
                    valueColor = thermal.cpuTempC?.let {
                        statusColorForTemperature(it)
                    } ?: MaterialTheme.colorScheme.onSurface,
                    onInfoClick = { onInfoClick("cpuTemp") },
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.thermal_headroom),
                    value = thermal.thermalHeadroom?.let {
                        stringResource(R.string.value_headroom_percent, formatDecimal((1f - it.coerceIn(0f, 1f)) * 100, 0))
                    } ?: stringResource(R.string.thermal_cpu_unavailable),
                    valueColor = thermal.thermalHeadroom?.let { headroom ->
                        headroomColor(headroom)
                    } ?: MaterialTheme.colorScheme.onSurface,
                    onInfoClick = { onInfoClick("thermalHeadroom") },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Row 2: Thermal Status + Throttling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
            ) {
                MetricPill(
                    label = stringResource(R.string.thermal_status),
                    value = thermalStatusLabel(thermal.thermalStatus),
                    valueColor = thermalStatusColor(thermal.thermalStatus),
                    onInfoClick = { onInfoClick("thermalStatus") },
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.thermal_throttling),
                    value = if (thermal.isThrottling) {
                        stringResource(R.string.thermal_throttling_active)
                    } else {
                        stringResource(R.string.thermal_throttling_none)
                    },
                    valueColor = if (thermal.isThrottling) {
                        MaterialTheme.statusColors.critical
                    } else {
                        MaterialTheme.statusColors.healthy
                    },
                    onInfoClick = { onInfoClick("throttling") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Throttling event list ────────────────────────────────────────────────────────

@Composable
private fun ThrottlingEmptyState() {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            StatusDot(color = MaterialTheme.statusColors.healthy)
            Text(
                text = stringResource(R.string.thermal_no_events),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThrottlingEventItem(
    event: ThrottlingEvent,
    temperatureUnit: TemperatureUnit
) {
    val formattedTime = rememberFormattedDateTime(event.timestamp, "yMMMdHm")
    val statusColor = when (event.thermalStatus.lowercase()) {
        "severe" -> MaterialTheme.statusColors.poor
        "critical", "emergency", "shutdown" -> MaterialTheme.statusColors.critical
        else -> MaterialTheme.statusColors.fair
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusDot(color = statusColor)
                    Text(
                        text = event.thermalStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = formatTemperature(event.batteryTempC, temperatureUnit),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MaterialTheme.numericFontFamily
                    ),
                    color = statusColorForTemperature(event.batteryTempC)
                )
            }

            event.cpuTempC?.let { cpuTemp ->
                Text(
                    text = stringResource(
                        R.string.value_label_colon,
                        stringResource(R.string.thermal_cpu_temp),
                        formatTemperature(cpuTemp, temperatureUnit)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            event.foregroundApp?.let { app ->
                Text(
                    text = app,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            event.durationMs?.let { duration ->
                val minutes = duration / 60_000
                val seconds = (duration % 60_000) / 1000
                val durationText = if (minutes > 0) {
                    stringResource(R.string.value_duration_minutes_seconds, minutes, seconds)
                } else {
                    stringResource(R.string.value_duration_seconds, seconds)
                }
                Text(
                    text = stringResource(R.string.thermal_event_duration, durationText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Helper functions ─────────────────────────────────────────────────────────────

@Composable
private fun thermalStatusLabel(status: ThermalStatus): String = when (status) {
    ThermalStatus.NONE -> stringResource(R.string.thermal_status_none)
    ThermalStatus.LIGHT -> stringResource(R.string.thermal_status_light)
    ThermalStatus.MODERATE -> stringResource(R.string.thermal_status_moderate)
    ThermalStatus.SEVERE -> stringResource(R.string.thermal_status_severe)
    ThermalStatus.CRITICAL -> stringResource(R.string.thermal_status_critical)
    ThermalStatus.EMERGENCY -> stringResource(R.string.thermal_status_emergency)
    ThermalStatus.SHUTDOWN -> stringResource(R.string.thermal_status_shutdown)
}

@Composable
private fun thermalStatusColor(status: ThermalStatus): Color {
    val colors = MaterialTheme.statusColors
    return when (status) {
        ThermalStatus.NONE -> colors.healthy
        ThermalStatus.LIGHT -> colors.healthy
        ThermalStatus.MODERATE -> colors.fair
        ThermalStatus.SEVERE -> colors.poor
        ThermalStatus.CRITICAL -> colors.critical
        ThermalStatus.EMERGENCY -> colors.critical
        ThermalStatus.SHUTDOWN -> colors.critical
    }
}

@Composable
private fun headroomColor(headroom: Float): Color {
    val usedPercent = ((1f - headroom.coerceIn(0f, 1f)) * 100).toInt()
    val colors = MaterialTheme.statusColors
    return when {
        usedPercent >= 90 -> colors.critical
        usedPercent >= 70 -> colors.poor
        usedPercent >= 50 -> colors.fair
        else -> colors.healthy
    }
}
