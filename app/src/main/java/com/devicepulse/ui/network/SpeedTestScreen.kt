package com.devicepulse.ui.network

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.NetworkState
import com.devicepulse.domain.model.SpeedTestResult
import com.devicepulse.ui.components.AnimatedFloatText
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.common.formatDecimal
import com.devicepulse.ui.theme.DevicePulseTheme
import com.devicepulse.ui.theme.reducedMotion
import com.devicepulse.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        DetailTopBar(
            title = androidx.compose.ui.res.stringResource(R.string.speed_test_title),
            onBack = onBack
        )

        when (val networkState = uiState) {
            is NetworkUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }

            is NetworkUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.error_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            is NetworkUiState.Success -> {
                SpeedTestContent(
                    networkState = networkState.networkState,
                    speedTestState = speedTestState,
                    isCellular = networkState.networkState.connectionType == ConnectionType.CELLULAR,
                    onStartSpeedTest = { viewModel.startSpeedTest() }
                )
            }
        }
    }
}

@Composable
private fun SpeedTestContent(
    networkState: com.devicepulse.domain.model.NetworkState,
    speedTestState: SpeedTestUiState,
    isCellular: Boolean,
    onStartSpeedTest: () -> Unit
) {
    var showCellularWarning by remember { mutableStateOf(false) }
    val hasConnection = networkState.connectionType != ConnectionType.NONE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        NetworkContextPanel(networkState = networkState)

        SpeedTestHero(
            state = speedTestState,
            enabled = hasConnection && !speedTestState.isRunning,
            onStart = {
                if (isCellular) {
                    showCellularWarning = true
                } else {
                    onStartSpeedTest()
                }
            }
        )

        Text(
            text = heroInstructionText(
                hasConnection = hasConnection,
                phase = speedTestState.phase
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        SpeedMetricsStrip(state = speedTestState)

        ConnectionStatsRow(state = speedTestState)

        if (speedTestState.phase is SpeedTestPhase.Failed) {
            SpeedTestFailureCard(
                error = speedTestState.phase.error
            )
        }

        speedTestState.historyLoadError?.let { error ->
            SpeedTestFailureCard(error = error)
        }

        speedTestState.lastResult?.let { result ->
            LatestResultCard(result = result)
        }

        if (speedTestState.recentResults.size > 1) {
            HistorySection(results = speedTestState.recentResults.drop(1))
        }

        Text(
            text = androidx.compose.ui.res.stringResource(R.string.speed_test_mlab_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.xl)
        )
    }

    if (showCellularWarning) {
        CellularDataWarningDialog(
            onConfirm = {
                showCellularWarning = false
                onStartSpeedTest()
            },
            onDismiss = { showCellularWarning = false }
        )
    }
}

@Composable
private fun NetworkContextPanel(networkState: com.devicepulse.domain.model.NetworkState) {
    val connectionLabel = when (networkState.connectionType) {
        ConnectionType.WIFI -> networkState.wifiSsid ?: androidx.compose.ui.res.stringResource(R.string.connection_wifi)
        ConnectionType.CELLULAR -> networkState.networkSubtype ?: androidx.compose.ui.res.stringResource(R.string.connection_cellular)
        ConnectionType.NONE -> androidx.compose.ui.res.stringResource(R.string.network_no_connection)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.speed_test_connection),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = connectionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            networkState.latencyMs?.let { latency ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.speed_test_ping),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$latency ${androidx.compose.ui.res.stringResource(R.string.unit_ms)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedTestHero(
    state: SpeedTestUiState,
    enabled: Boolean,
    onStart: () -> Unit
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val infiniteTransition = rememberInfiniteTransition(label = "speed_test_hero")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 0 else 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speed_test_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.26f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 0 else 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speed_test_pulse_alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 0 else 1800,
                easing = LinearEasing
            )
        ),
        label = "speed_test_rotation"
    )

    val accent = MaterialTheme.colorScheme.primary
    val secondaryAccent = lerp(accent, Color.White, 0.18f)
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val errorAccent = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val surfaceTone = MaterialTheme.colorScheme.surface
    val surfaceContainerTone = MaterialTheme.colorScheme.surfaceContainerHigh

    val targetProgress = when (state.phase) {
        SpeedTestPhase.Idle -> 0f
        SpeedTestPhase.Ping -> 0.18f
        SpeedTestPhase.Download -> state.downloadProgress.coerceIn(0f, 1f)
        SpeedTestPhase.Upload -> state.uploadProgress.coerceIn(0f, 1f)
        SpeedTestPhase.Completed -> 1f
        is SpeedTestPhase.Failed -> 0f
    }

    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = if (reducedMotion) 0 else 700, easing = FastOutSlowInEasing),
        label = "speed_test_ring_progress"
    )

    val centerValue = when (state.phase) {
        SpeedTestPhase.Download -> state.downloadMbps.toFloat()
        SpeedTestPhase.Upload -> state.uploadMbps.toFloat()
        SpeedTestPhase.Completed -> state.downloadMbps.toFloat()
        else -> 0f
    }

    val centerLabel = when (state.phase) {
        SpeedTestPhase.Download -> androidx.compose.ui.res.stringResource(R.string.speed_test_download)
        SpeedTestPhase.Upload -> androidx.compose.ui.res.stringResource(R.string.speed_test_upload)
        SpeedTestPhase.Completed -> androidx.compose.ui.res.stringResource(R.string.speed_test_download)
        else -> ""
    }
    val ringActionLabel = heroInstructionText(
        hasConnection = enabled || state.isRunning,
        phase = state.phase
    )

    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .size(286.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(248.dp)
                .scale(if (state.phase == SpeedTestPhase.Idle) pulseScale else 1f)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (state.phase == SpeedTestPhase.Idle) pulseAlpha else 0.08f))
        )

        Canvas(
            modifier = Modifier
                .size(248.dp)
                .clip(CircleShape)
                .semantics {
                    role = Role.Button
                    contentDescription = ringActionLabel
                }
                .clickable(enabled = enabled) { onStart() }
        ) {
            val stroke = 16.dp.toPx()
            val outerStroke = 2.dp.toPx()
            val size = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.16f),
                        surfaceContainerTone.copy(alpha = 0.88f),
                        surfaceTone.copy(alpha = 0.96f)
                    )
                ),
                radius = this.size.minDimension / 2.3f
            )

            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            when (state.phase) {
                SpeedTestPhase.Idle -> {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                accent.copy(alpha = 0.0f),
                                accent.copy(alpha = 0.85f),
                                secondaryAccent.copy(alpha = 0.55f),
                                accent.copy(alpha = 0.0f)
                            )
                        ),
                        startAngle = 130f,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                SpeedTestPhase.Ping -> {
                    rotate(rotation) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    accent.copy(alpha = 0.0f),
                                    secondaryAccent,
                                    accent,
                                    accent.copy(alpha = 0.0f)
                                )
                            ),
                            startAngle = 145f,
                            sweepAngle = 112f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = size,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }

                is SpeedTestPhase.Failed -> {
                    drawArc(
                        color = errorAccent,
                        startAngle = 135f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }

                else -> {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                lerp(accent, Color.White, 0.06f),
                                accent,
                                lerp(accent, Color.Black, 0.08f),
                                lerp(accent, Color.White, 0.05f)
                            )
                        ),
                        startAngle = 135f,
                        sweepAngle = 270f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = size,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }

            drawArc(
                color = accent.copy(alpha = 0.18f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(topLeft.x + 14.dp.toPx(), topLeft.y + 14.dp.toPx()),
                size = Size(size.width - 28.dp.toPx(), size.height - 28.dp.toPx()),
                style = Stroke(width = outerStroke, cap = StrokeCap.Round)
            )
        }

        if (state.phase != SpeedTestPhase.Idle && state.phase !is SpeedTestPhase.Failed) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedFloatText(
                    value = centerValue,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontFamily = FontFamily.Default,
                        fontSize = 40.sp,
                        lineHeight = 44.sp
                    ),
                    decimalPlaces = 1
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.unit_mbps),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CellularDataWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = androidx.compose.ui.res.stringResource(R.string.speed_test_title))
        },
        text = {
            Text(text = androidx.compose.ui.res.stringResource(R.string.speed_test_cellular_warning))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = androidx.compose.ui.res.stringResource(R.string.speed_test_cellular_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = androidx.compose.ui.res.stringResource(R.string.speed_test_cellular_cancel))
            }
        }
    )
}

@Composable
private fun SpeedMetricsStrip(state: SpeedTestUiState) {
    val accent = MaterialTheme.colorScheme.primary
    val softAccent = lerp(accent, Color.White, 0.16f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        MetricGlowCard(
            title = androidx.compose.ui.res.stringResource(R.string.speed_test_download),
            value = state.downloadMbps.toFloat(),
            unit = androidx.compose.ui.res.stringResource(R.string.unit_mbps),
            accent = accent,
            modifier = Modifier.weight(1f)
        )
        MetricGlowCard(
            title = androidx.compose.ui.res.stringResource(R.string.speed_test_upload),
            value = state.uploadMbps.toFloat(),
            unit = androidx.compose.ui.res.stringResource(R.string.unit_mbps),
            accent = softAccent,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricGlowCard(
    title: String,
    value: Float,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = accent
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedFloatText(
                    value = value,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Normal
                    ),
                    decimalPlaces = 1
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatsRow(state: SpeedTestUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        StatPill(
            label = androidx.compose.ui.res.stringResource(R.string.speed_test_ping),
            value = if (state.pingMs > 0) state.pingMs.toString() else "—",
            unit = androidx.compose.ui.res.stringResource(R.string.unit_ms),
            modifier = Modifier.weight(1f)
        )
        StatPill(
            label = androidx.compose.ui.res.stringResource(R.string.speed_test_jitter),
            value = if (state.jitterMs > 0) state.jitterMs.toString() else "—",
            unit = androidx.compose.ui.res.stringResource(R.string.unit_ms),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeedTestFailureCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(MaterialTheme.spacing.base)
        )
    }
}

@Composable
private fun LatestResultCard(result: SpeedTestResult) {
    val dateLabel = rememberTimestampLabel(
        timestamp = result.timestamp,
        pattern = "dd.MM HH:mm"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.speed_test_last_result),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                ResultColumn(
                    label = androidx.compose.ui.res.stringResource(R.string.speed_test_download),
                    value = result.downloadMbps,
                    unit = androidx.compose.ui.res.stringResource(R.string.unit_mbps),
                    modifier = Modifier.weight(1f)
                )
                ResultColumn(
                    label = androidx.compose.ui.res.stringResource(R.string.speed_test_upload),
                    value = result.uploadMbps,
                    unit = androidx.compose.ui.res.stringResource(R.string.unit_mbps),
                    modifier = Modifier.weight(1f)
                )
                ResultColumn(
                    label = androidx.compose.ui.res.stringResource(R.string.speed_test_ping),
                    value = result.pingMs.toDouble(),
                    unit = androidx.compose.ui.res.stringResource(R.string.unit_ms),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ResultColumn(
    label: String,
    value: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = if (unit == androidx.compose.ui.res.stringResource(R.string.unit_ms)) {
                    value.roundToInt().toString()
                } else {
                    formatDecimal(value, 1)
                },
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Default),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistorySection(results: List<SpeedTestResult>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.speed_test_history),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        results.forEach { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.spacing.base, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rememberTimestampLabel(
                            timestamp = result.timestamp,
                            pattern = "dd.MM HH:mm"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        TinyHistoryMetric(result.downloadMbps, androidx.compose.ui.res.stringResource(R.string.speed_test_download))
                        TinyHistoryMetric(result.uploadMbps, androidx.compose.ui.res.stringResource(R.string.speed_test_upload))
                    }
                }
            }
        }
    }
}

@Composable
private fun TinyHistoryMetric(value: Double, label: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = formatDecimal(value, 0),
            style = MaterialTheme.typography.titleSmall.copy(fontFamily = FontFamily.Default),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun rememberTimestampLabel(timestamp: Long, pattern: String): String {
    val formatter = remember(pattern) {
        java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
    }
    return remember(timestamp, formatter) {
        formatter.format(java.util.Date(timestamp))
    }
}

@Composable
private fun heroInstructionText(
    hasConnection: Boolean,
    phase: SpeedTestPhase
): String {
    return when {
        !hasConnection -> androidx.compose.ui.res.stringResource(R.string.speed_test_no_connection_hint)
        phase == SpeedTestPhase.Idle -> androidx.compose.ui.res.stringResource(R.string.speed_test_tap_ring_start)
        phase == SpeedTestPhase.Ping -> androidx.compose.ui.res.stringResource(R.string.speed_test_phase_ping)
        phase == SpeedTestPhase.Download -> androidx.compose.ui.res.stringResource(R.string.speed_test_phase_download)
        phase == SpeedTestPhase.Upload -> androidx.compose.ui.res.stringResource(R.string.speed_test_phase_upload)
        phase == SpeedTestPhase.Completed -> androidx.compose.ui.res.stringResource(R.string.speed_test_tap_ring_restart)
        phase is SpeedTestPhase.Failed -> androidx.compose.ui.res.stringResource(R.string.speed_test_tap_ring_retry)
        else -> ""
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun SpeedTestContentPreview() {
    DevicePulseTheme {
        SpeedTestContent(
            networkState = NetworkState(
                connectionType = ConnectionType.WIFI,
                signalDbm = -54,
                signalQuality = com.devicepulse.domain.model.SignalQuality.EXCELLENT,
                wifiSsid = "Lab 6E",
                latencyMs = 16
            ),
            speedTestState = SpeedTestUiState(
                phase = SpeedTestPhase.Completed,
                downloadMbps = 542.7,
                uploadMbps = 83.4,
                pingMs = 17,
                jitterMs = 3,
                lastResult = SpeedTestResult(
                    timestamp = 1_710_000_000_000,
                    downloadMbps = 542.7,
                    uploadMbps = 83.4,
                    pingMs = 17,
                    jitterMs = 3,
                    serverName = "M-Lab Helsinki",
                    serverLocation = "Helsinki",
                    connectionType = ConnectionType.WIFI,
                    networkSubtype = null,
                    signalDbm = -54
                ),
                recentResults = listOf(
                    SpeedTestResult(
                        timestamp = 1_710_000_000_000,
                        downloadMbps = 542.7,
                        uploadMbps = 83.4,
                        pingMs = 17,
                        jitterMs = 3,
                        serverName = "M-Lab Helsinki",
                        serverLocation = "Helsinki",
                        connectionType = ConnectionType.WIFI,
                        networkSubtype = null,
                        signalDbm = -54
                    ),
                    SpeedTestResult(
                        timestamp = 1_709_999_000_000,
                        downloadMbps = 518.2,
                        uploadMbps = 79.9,
                        pingMs = 19,
                        jitterMs = 4,
                        serverName = "M-Lab Stockholm",
                        serverLocation = "Stockholm",
                        connectionType = ConnectionType.WIFI,
                        networkSubtype = null,
                        signalDbm = -57
                    )
                )
            ),
            isCellular = false,
            onStartSpeedTest = {}
        )
    }
}
