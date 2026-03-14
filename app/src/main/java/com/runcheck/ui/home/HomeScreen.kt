package com.runcheck.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.runcheck.R
import com.runcheck.domain.model.HealthScore
import com.runcheck.domain.model.HealthStatus
import com.runcheck.ui.common.batteryHealthLabel
import com.runcheck.ui.common.chargingStatusLabel
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatPercent
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.common.plugTypeLabel
import com.runcheck.ui.common.scoreLabel
import com.runcheck.ui.common.temperatureBandLabel
import com.runcheck.ui.components.GridCard
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.ProBadgePill
import com.runcheck.ui.components.ProgressRing
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.StatusDot
import com.runcheck.ui.theme.AccentBlue
import com.runcheck.ui.theme.AccentOrange
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.statusColors
import com.runcheck.ui.theme.statusColorForPercent
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColorForTemperature
import com.runcheck.ui.theme.BgIconCircle
import com.runcheck.ui.theme.BgCardAlt
import com.runcheck.ui.theme.TextMuted
import com.runcheck.ui.theme.TextSecondary
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.statusColors
import kotlin.math.sin

@Composable
fun HomeScreen(
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
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
        PrimaryTopBar(
            title = stringResource(R.string.app_name),
            actions = {
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings_title),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        when (val state = uiState) {
            is HomeUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is HomeUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.common_error_generic),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is HomeUiState.Success -> {
                HomeContent(
                    state = state,
                    onNavigateToBattery = onNavigateToBattery,
                    onNavigateToNetwork = onNavigateToNetwork,
                    onNavigateToThermal = onNavigateToThermal,
                    onNavigateToStorage = onNavigateToStorage,
                    onNavigateToCharger = onNavigateToCharger,
                    onNavigateToSpeedTest = onNavigateToSpeedTest,
                    onNavigateToAppUsage = onNavigateToAppUsage,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToProUpgrade = onNavigateToProUpgrade
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProUpgrade: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        HomeStatusSummary(state = state)

        Spacer(modifier = Modifier.height(12.dp))

        HealthScoreCard(
            healthScore = state.healthScore,
            batteryTempC = state.thermalState.batteryTempC,
            onNavigateToBattery = onNavigateToBattery,
            onNavigateToThermal = onNavigateToThermal,
            onNavigateToNetwork = onNavigateToNetwork,
            onNavigateToStorage = onNavigateToStorage
        )

        Spacer(modifier = Modifier.height(12.dp))

        BatteryHeroCard(
            battery = state.batteryState,
            onClick = onNavigateToBattery
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GridCard(
                icon = Icons.Outlined.SignalCellularAlt,
                title = stringResource(R.string.home_network_card),
                subtitle = connectionDisplayLabel(
                    connectionType = state.networkState.connectionType,
                    wifiSsid = state.networkState.wifiSsid,
                    networkSubtype = state.networkState.networkSubtype
                ),
                subtitleColor = MaterialTheme.colorScheme.onSurface,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToNetwork,
                modifier = Modifier.weight(1f)
            )
            GridCard(
                icon = Icons.Outlined.Thermostat,
                title = stringResource(R.string.home_thermal_card),
                subtitle = stringResource(
                    R.string.home_thermal_summary,
                    formatTemperature(state.thermalState.batteryTempC),
                    temperatureBandLabel(state.thermalState.batteryTempC)
                ),
                subtitleColor = statusColorForTemperature(state.thermalState.batteryTempC),
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToThermal,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GridCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = stringResource(R.string.home_chargers_card),
                subtitle = stringResource(R.string.home_test_compare),
                subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                locked = !state.isPro,
                onClick = if (state.isPro) onNavigateToCharger else onNavigateToProUpgrade,
                modifier = Modifier.weight(1f)
            )
            GridCard(
                icon = Icons.Outlined.DataUsage,
                title = stringResource(R.string.home_storage_card),
                subtitle = stringResource(
                    R.string.home_storage_free,
                    formatStorageSize(context, state.storageState.availableBytes),
                    stringResource(R.string.home_free_suffix)
                ),
                subtitleColor = statusColorForStoragePercent(
                    ((state.storageState.totalBytes - state.storageState.availableBytes) * 100 /
                        state.storageState.totalBytes.coerceAtLeast(1)).toInt()
                ),
                iconBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                onClick = onNavigateToStorage,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(stringResource(R.string.home_quick_tools))

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                ListRow(
                    label = stringResource(R.string.home_speed_test),
                    icon = Icons.Outlined.Speed,
                    onClick = onNavigateToSpeedTest
                )
                HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
                Box(modifier = Modifier.fillMaxWidth()) {
                    ListRow(
                        label = stringResource(R.string.home_app_usage_card),
                        icon = Icons.Outlined.DataUsage,
                        onClick = if (state.isPro) onNavigateToAppUsage else onNavigateToProUpgrade,
                        trailing = if (!state.isPro) {
                            { ProBadgePill() }
                        } else null
                    )

                    if (!state.isPro) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.14f))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!state.isPro) {
            Card(
                onClick = onNavigateToProUpgrade,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_pro_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_pro_history_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentTeal.copy(alpha = 0.16f)
                    ) {
                        IconCircle(icon = Icons.Outlined.Star)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_insights_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_insights_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HomeStatusSummary(state: HomeUiState.Success) {
    val batteryStatus = chargingStatusLabel(state.batteryState.chargingStatus)
    val networkStatus = connectionDisplayLabel(
        connectionType = state.networkState.connectionType,
        wifiSsid = state.networkState.wifiSsid,
        networkSubtype = state.networkState.networkSubtype
    )
    val temperatureBand = temperatureBandLabel(state.thermalState.batteryTempC)

    Text(
        text = stringResource(
            R.string.home_status_summary,
            batteryStatus,
            temperatureBand,
            networkStatus
        ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HealthScoreCard(
    healthScore: HealthScore,
    batteryTempC: Float,
    onNavigateToBattery: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToStorage: () -> Unit
) {
    val score = healthScore.overallScore
    val statusLabel = scoreLabel(score)
    val formattedSummary = stringResource(R.string.home_device_good_shape, statusLabel.lowercase())
    val statusWord = statusLabel.lowercase()
    val annotatedSummary = remember(formattedSummary, statusWord) {
        buildAnnotatedString {
            val startIndex = formattedSummary.indexOf(statusWord)
            if (startIndex >= 0) {
                append(formattedSummary.substring(0, startIndex))
                withStyle(SpanStyle(color = AccentTeal, fontWeight = FontWeight.Bold)) {
                    append(statusWord)
                }
                append(formattedSummary.substring(startIndex + statusWord.length))
            } else {
                append(formattedSummary)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionHeader(stringResource(R.string.home_health_score))
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProgressRing(
                progress = score / 100f,
                modifier = Modifier.size(152.dp),
                strokeWidth = 10.dp,
                progressColor = statusColorForPercent(score),
                contentDescription = stringResource(
                    R.string.a11y_progress_percent,
                    stringResource(R.string.home_health_score),
                    score
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = MaterialTheme.numericFontFamily,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = annotatedSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            if (batteryTempC >= 38f) {
                Text(
                    text = stringResource(R.string.home_temp_elevated),
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentOrange
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            HealthBreakdownRow(
                label = stringResource(R.string.home_battery_card),
                value = formatPercent(healthScore.batteryScore),
                status = HealthScore.statusFromScore(healthScore.batteryScore),
                onClick = onNavigateToBattery
            )
            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            HealthBreakdownRow(
                label = stringResource(R.string.home_thermal_card),
                value = formatPercent(healthScore.thermalScore),
                status = HealthScore.statusFromScore(healthScore.thermalScore),
                onClick = onNavigateToThermal
            )
            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            HealthBreakdownRow(
                label = stringResource(R.string.home_network_card),
                value = formatPercent(healthScore.networkScore),
                status = HealthScore.statusFromScore(healthScore.networkScore),
                onClick = onNavigateToNetwork
            )
            HorizontalDivider(color = BgIconCircle, thickness = 1.dp)
            HealthBreakdownRow(
                label = stringResource(R.string.home_storage_card),
                value = formatPercent(healthScore.storageScore),
                status = HealthScore.statusFromScore(healthScore.storageScore),
                onClick = onNavigateToStorage
            )
        }
    }
}

@Composable
private fun BatteryHeroCard(
    battery: com.runcheck.domain.model.BatteryState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            SectionHeader(stringResource(R.string.home_battery_card))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = battery.level.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = MaterialTheme.numericFontFamily,
                                fontSize = 54.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.unit_percent),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = MaterialTheme.numericFontFamily
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 2.dp, bottom = 11.dp)
                        )
                    }
                    Text(
                        text = chargingStatusLabel(battery.chargingStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                HomeBatteryChargeIcon(
                    level = battery.level,
                    isCharging = battery.chargingStatus == com.runcheck.domain.model.ChargingStatus.CHARGING,
                    progress = battery.level / 100f,
                    modifier = Modifier.size(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricPill(
                    label = stringResource(R.string.battery_health),
                    value = batteryHealthLabel(battery.health),
                    valueColor = if (battery.health == com.runcheck.domain.model.BatteryHealth.GOOD) {
                        MaterialTheme.statusColors.healthy
                    } else {
                        MaterialTheme.statusColors.fair
                    },
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = stringResource(R.string.battery_plug_type),
                    value = plugTypeLabel(battery.plugType),
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HealthBreakdownRow(
    label: String,
    value: String,
    status: HealthStatus,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .semantics(mergeDescendants = true) {}
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(
            color = statusColor(status),
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = MaterialTheme.numericFontFamily
            ),
            color = TextSecondary
        )
    }
}

@Composable
private fun HomeBatteryChargeIcon(
    level: Int,
    isCharging: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val fillLevel = progress.coerceIn(0f, 1f)
    val reducedMotion = MaterialTheme.reducedMotion

    val wavePhase = if (isCharging && !reducedMotion && fillLevel in 0.01f..0.99f) {
        val infiniteTransition = rememberInfiniteTransition(label = "batteryWave")
        val phase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing)
            ),
            label = "wavePhase"
        )
        phase
    } else {
        0f
    }

    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = statusColorForPercent(level)
    val textColor = if (fillLevel > 0.55f) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.size(width = 80.dp, height = 124.dp)
        ) {
            val capWidth = 18.dp.toPx()
            val capHeight = 5.dp.toPx()
            val capRadius = 2.dp.toPx()
            val bodyTop = capHeight + 1.dp.toPx()
            val bodyHeight = size.height - bodyTop
            val strokeW = 2.dp.toPx()
            val cornerRadius = 12.dp.toPx()
            val inset = 4.dp.toPx()
            val waveAmplitude = 3.dp.toPx()

            // Terminal cap
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset((size.width - capWidth) / 2f, 0f),
                size = Size(capWidth, capHeight),
                cornerRadius = CornerRadius(capRadius, capRadius)
            )

            // Battery body outline
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset(0f, bodyTop),
                size = Size(size.width, bodyHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = strokeW)
            )

            // Fill area
            if (fillLevel > 0f) {
                val fillAreaTop = bodyTop + strokeW + inset
                val fillAreaBottom = bodyTop + bodyHeight - strokeW - inset
                val fillAreaLeft = strokeW + inset
                val fillAreaRight = size.width - strokeW - inset
                val fillAreaHeight = fillAreaBottom - fillAreaTop
                val fillTop = fillAreaBottom - fillAreaHeight * fillLevel
                val fillCorner = 8.dp.toPx()

                clipPath(Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = fillAreaLeft,
                            top = fillAreaTop,
                            right = fillAreaRight,
                            bottom = fillAreaBottom,
                            cornerRadius = CornerRadius(fillCorner, fillCorner)
                        )
                    )
                }) {
                    val showWave = isCharging && !reducedMotion && fillLevel in 0.01f..0.99f

                    if (showWave) {
                        val wavePath = Path().apply {
                            moveTo(fillAreaLeft, fillTop)
                            val waveWidth = fillAreaRight - fillAreaLeft
                            val steps = 40
                            for (i in 0..steps) {
                                val x = fillAreaLeft + waveWidth * i / steps
                                val y = fillTop + waveAmplitude * sin(
                                    wavePhase + 2f * Math.PI.toFloat() * i / steps
                                )
                                lineTo(x, y)
                            }
                            lineTo(fillAreaRight, fillAreaBottom)
                            lineTo(fillAreaLeft, fillAreaBottom)
                            close()
                        }
                        drawPath(
                            path = wavePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(fillColor, fillColor.copy(alpha = 0.6f)),
                                startY = fillTop,
                                endY = fillAreaBottom
                            )
                        )
                    } else {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(fillColor, fillColor.copy(alpha = 0.6f)),
                                startY = fillTop,
                                endY = fillAreaBottom
                            ),
                            topLeft = Offset(fillAreaLeft, fillTop),
                            size = Size(fillAreaRight - fillAreaLeft, fillAreaBottom - fillTop)
                        )
                    }
                }
            }
        }

        // Overlaid level text (participates in semantics tree)
        Text(
            text = level.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = MaterialTheme.numericFontFamily,
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}


@Composable
private fun statusColor(status: HealthStatus): Color {
    val colors = MaterialTheme.statusColors
    return when (status) {
        HealthStatus.HEALTHY -> colors.healthy
        HealthStatus.FAIR -> colors.fair
        HealthStatus.POOR -> colors.poor
        HealthStatus.CRITICAL -> colors.critical
    }
}
