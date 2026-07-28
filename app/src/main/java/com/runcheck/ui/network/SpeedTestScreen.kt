package com.runcheck.ui.network

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.ui.common.LifecycleStartStopEffect
import com.runcheck.ui.common.connectionDisplayLabel
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.measurementMotionPolicy
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.AnimatedCounter
import com.runcheck.ui.components.AnimatedFloatText
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.RuncheckDetailScaffold
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.info.InfoSheetHost
import com.runcheck.ui.components.info.rememberInfoSheetState
import com.runcheck.ui.components.rememberMeasurementIsResumed
import com.runcheck.ui.theme.MotionTokens
import com.runcheck.ui.theme.PreviewsRuncheckThemes
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.domainColors
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericSpeedHeroValueTextStyle
import com.runcheck.ui.theme.reducedMotion
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun SpeedTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NetworkViewModel = hiltViewModel(),
) {
    val networkUiState by viewModel.networkUiState.collectAsStateWithLifecycle()
    val speedTestState by viewModel.speedTestState.collectAsStateWithLifecycle()
    val loadingDescription = stringResource(R.string.a11y_loading)

    LifecycleStartStopEffect(
        onStart = viewModel::startObserving,
        onStop = viewModel::stopObserving,
    )

    RuncheckDetailScaffold(
        title = stringResource(R.string.speed_test_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (val netState = networkUiState) {
            is NetworkUiState.Loading -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics {
                                contentDescription = loadingDescription
                                liveRegion = LiveRegionMode.Polite
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    RuncheckProgressSpinner(contentDescription = loadingDescription)
                }
            }

            is NetworkUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = netState.message.resolve(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            is NetworkUiState.Success -> {
                SpeedTestContent(
                    networkState = netState.networkState,
                    speedTestState = speedTestState,
                    onStartSpeedTest = { viewModel.startSpeedTest() },
                    onStopSpeedTest = { viewModel.stopSpeedTest() },
                    onConfirmCellular = { viewModel.confirmCellularSpeedTest() },
                    onDismissCellular = { viewModel.dismissCellularWarning() },
                )
            }
        }
    }
}

@Composable
private fun SpeedTestContent(
    networkState: NetworkState,
    speedTestState: SpeedTestUiState,
    onStartSpeedTest: () -> Unit,
    onStopSpeedTest: () -> Unit,
    onConfirmCellular: () -> Unit,
    onDismissCellular: () -> Unit,
) {
    val activeInfoSheetState = rememberInfoSheetState()
    val hasConnection = networkState.connectionType != ConnectionType.NONE

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

        NetworkContextPanel(networkState = networkState)

        SpeedTestHero(
            state = speedTestState,
            enabled = hasConnection || speedTestState.isRunning,
            onAction = if (speedTestState.isRunning) onStopSpeedTest else onStartSpeedTest,
        )

        SpeedTestPhaseTrail(phase = speedTestState.phase)

        Text(
            text =
                heroInstructionText(
                    hasConnection = hasConnection,
                    phase = speedTestState.phase,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )

        // Combined metrics card (Download, Upload, Ping, Jitter)
        SpeedMetricsCard(
            state = speedTestState,
            onInfoClick = { key -> activeInfoSheetState.value = key },
        )

        if (speedTestState.phase is SpeedTestPhase.Failed) {
            SpeedTestFailureCard(error = speedTestState.phase.error.resolve())
        }

        speedTestState.historyLoadError?.let { error ->
            SpeedTestFailureCard(error = error.resolve())
        }

        speedTestState.lastResult?.let { result ->
            LatestResultCard(result = result)
        }

        if (speedTestState.recentResults.size > 1) {
            HistorySection(results = speedTestState.recentResults.drop(1))
        } else if (speedTestState.phase == SpeedTestPhase.Idle && speedTestState.lastResult == null) {
            EmptyHistoryCard()
        }

        Text(
            text = stringResource(R.string.speed_test_mlab_notice),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.xl),
        )
    }

    if (speedTestState.showCellularWarning) {
        CellularDataWarningDialog(
            onConfirm = onConfirmCellular,
            onDismiss = onDismissCellular,
        )
    }

    InfoSheetHost(
        activeKey = activeInfoSheetState.value,
        onDismiss = { activeInfoSheetState.value = null },
        resolveContent = ::resolveSpeedTestInfoContent,
    )
}

private fun resolveSpeedTestInfoContent(key: String) =
    when (key) {
        "download" -> SpeedTestInfoContent.download
        "upload" -> SpeedTestInfoContent.upload
        "ping" -> SpeedTestInfoContent.ping
        "jitter" -> SpeedTestInfoContent.jitter
        else -> null
    }

// ── Network context panel ────────────────────────────────────────────────────────

@Composable
private fun NetworkContextPanel(networkState: NetworkState) {
    val connectionLabel =
        when (networkState.connectionType) {
            ConnectionType.NONE -> {
                stringResource(R.string.network_no_connection)
            }

            ConnectionType.WIFI,
            ConnectionType.CELLULAR,
            ConnectionType.VPN,
            -> {
                connectionDisplayLabel(
                    connectionType = networkState.connectionType,
                    wifiSsid = networkState.wifiSsid,
                    networkSubtype = networkState.networkSubtype,
                )
            }
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricPill(
                label = stringResource(R.string.speed_test_connection),
                value = connectionLabel,
            )
            networkState.latencyMs?.let { latency ->
                MetricPill(
                    label = stringResource(R.string.speed_test_ping),
                    value =
                        stringResource(
                            R.string.value_with_unit_int,
                            latency,
                            stringResource(R.string.unit_ms),
                        ),
                )
            }
        }
    }
}

// ── Speed test hero ring ─────────────────────────────────────────────────────────

@Composable
private fun SpeedTestHero(
    state: SpeedTestUiState,
    enabled: Boolean,
    onAction: () -> Unit,
) {
    val reducedMotion = MaterialTheme.reducedMotion
    val measurementState = state.toMeasurementState()
    val motionPolicy =
        measurementMotionPolicy(
            state = measurementState,
            reducedMotion = reducedMotion,
            isResumed = rememberMeasurementIsResumed(),
        )

    val rotation =
        if (motionPolicy.showIndeterminateIndicator && state.phase == SpeedTestPhase.Ping) {
            val pingTransition = rememberInfiniteTransition(label = "speed_test_ping")
            pingTransition
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec =
                        infiniteRepeatable(
                            animation =
                                tween(
                                    durationMillis = MotionTokens.SPEED_SWEEP,
                                    easing = MotionTokens.SweepEasing,
                                ),
                        ),
                    label = "speed_test_rotation",
                ).value
        } else {
            0f
        }

    val accent = MaterialTheme.domainColors.network
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val errorAccent = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
    val surfaceContainerTone = MaterialTheme.colorScheme.surfaceContainerHigh

    val targetProgress =
        when (measurementState) {
            is com.runcheck.ui.common.MeasurementState.Sampling -> measurementState.progress ?: 0f
            com.runcheck.ui.common.MeasurementState.Result -> 1f
            else -> 0f
        }

    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec =
            tween(
                durationMillis = if (reducedMotion) 0 else MotionTokens.SPEED_RESULT,
                easing = FastOutSlowInEasing,
            ),
        label = "speed_test_ring_progress",
    )

    val centerValue =
        when (state.phase) {
            SpeedTestPhase.Idle -> 0f
            SpeedTestPhase.Ping -> 0f
            SpeedTestPhase.Download -> state.downloadMbps.toFloat()
            SpeedTestPhase.Upload -> state.uploadMbps.toFloat()
            SpeedTestPhase.Completed -> state.downloadMbps.toFloat()
            is SpeedTestPhase.Failed -> 0f
        }

    val centerLabel =
        when (state.phase) {
            SpeedTestPhase.Idle -> ""
            SpeedTestPhase.Ping -> ""
            SpeedTestPhase.Download -> stringResource(R.string.speed_test_download)
            SpeedTestPhase.Upload -> stringResource(R.string.speed_test_upload)
            SpeedTestPhase.Completed -> stringResource(R.string.speed_test_download)
            is SpeedTestPhase.Failed -> ""
        }
    val ringActionLabel =
        if (state.isRunning) {
            stringResource(R.string.speed_test_stop)
        } else {
            heroInstructionText(
                hasConnection = enabled,
                phase = state.phase,
            )
        }
    val actionCorner by animateDpAsState(
        targetValue = if (state.isRunning) 32.dp else 124.dp,
        animationSpec =
            tween(
                durationMillis = if (reducedMotion) 0 else MotionTokens.SHORT,
                easing = MotionTokens.EaseOut,
            ),
        label = "speed_test_action_corner",
    )
    val actionShape = RoundedCornerShape(actionCorner)

    Box(
        modifier =
            Modifier
                .padding(top = MaterialTheme.spacing.sm)
                .size(286.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(248.dp)
                    .clip(actionShape)
                    .background(surfaceContainerTone),
        )

        Canvas(
            modifier =
                Modifier
                    .size(248.dp)
                    .clip(actionShape)
                    .semantics {
                        role = Role.Button
                        contentDescription = ringActionLabel
                    }.clickable(enabled = enabled) { onAction() },
        ) {
            val stroke = 16.dp.toPx()
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)

            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            when (state.phase) {
                SpeedTestPhase.Idle -> {
                    drawArc(
                        color = accent,
                        startAngle = 130f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                SpeedTestPhase.Ping -> {
                    rotate(rotation) {
                        drawArc(
                            color = accent,
                            startAngle = 145f,
                            sweepAngle = 72f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
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
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                SpeedTestPhase.Download,
                SpeedTestPhase.Upload,
                SpeedTestPhase.Completed,
                -> {
                    drawArc(
                        color = accent,
                        startAngle = 135f,
                        sweepAngle = 270f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
        }

        when {
            state.phase == SpeedTestPhase.Idle -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.speed_test_start_button),
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = accent,
                    )
                }
            }

            state.phase == SpeedTestPhase.Completed -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedCounter(
                        value = centerValue,
                        style = MaterialTheme.numericSpeedHeroValueTextStyle,
                        formatter = { value -> formatDecimal(value, 1) },
                    )
                    Text(
                        text = stringResource(R.string.unit_mbps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                    Text(
                        text = stringResource(R.string.speed_test_run_again),
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = accent,
                    )
                }
            }

            state.phase !is SpeedTestPhase.Failed -> {
                val progressPercent =
                    when (state.phase) {
                        SpeedTestPhase.Download -> (state.downloadProgress * 100).roundToInt()
                        SpeedTestPhase.Upload -> (state.uploadProgress * 100).roundToInt()
                        else -> null
                    }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedFloatText(
                        value = centerValue,
                        style = MaterialTheme.numericSpeedHeroValueTextStyle,
                        decimalPlaces = 1,
                    )
                    Text(
                        text = stringResource(R.string.unit_mbps),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                    Text(
                        text =
                            if (progressPercent != null) {
                                stringResource(R.string.speed_test_progress, centerLabel, progressPercent)
                            } else {
                                centerLabel
                            },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.isRunning) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
                        Text(
                            text = stringResource(R.string.speed_test_stop),
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestPhaseTrail(phase: SpeedTestPhase) {
    val stages = phase.stageStates()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        stages.chunked(2).forEach { rowStages ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            ) {
                rowStages.forEach { (stage, state) ->
                    SpeedTestStageItem(
                        stage = stage,
                        state = state,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedTestStageItem(
    stage: SpeedTestStage,
    state: SpeedTestStageState,
    modifier: Modifier = Modifier,
) {
    val label =
        when (stage) {
            SpeedTestStage.PING -> stringResource(R.string.speed_test_ping)
            SpeedTestStage.DOWNLOAD -> stringResource(R.string.speed_test_download)
            SpeedTestStage.UPLOAD -> stringResource(R.string.speed_test_upload)
            SpeedTestStage.COMPLETED -> stringResource(R.string.speed_test_completed)
        }
    val stateLabel =
        when (state) {
            SpeedTestStageState.COMPLETED -> stringResource(R.string.speed_test_stage_state_completed)
            SpeedTestStageState.ACTIVE -> stringResource(R.string.speed_test_stage_state_active)
            SpeedTestStageState.UPCOMING -> stringResource(R.string.speed_test_stage_state_upcoming)
        }
    val stageDescription = stringResource(R.string.speed_test_stage_description, label, stateLabel)
    val accent = MaterialTheme.domainColors.network
    val icon =
        when (state) {
            SpeedTestStageState.COMPLETED -> Icons.Outlined.CheckCircle
            SpeedTestStageState.ACTIVE -> Icons.Outlined.RadioButtonChecked
            SpeedTestStageState.UPCOMING -> Icons.Outlined.RadioButtonUnchecked
        }
    val contentColor =
        when (state) {
            SpeedTestStageState.ACTIVE -> accent
            SpeedTestStageState.COMPLETED -> MaterialTheme.colorScheme.onSurface
            SpeedTestStageState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Row(
        modifier =
            modifier.clearAndSetSemantics {
                contentDescription = stageDescription
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight =
                if (state == SpeedTestStageState.ACTIVE) {
                    FontWeight.Bold
                } else {
                    FontWeight.Normal
                },
        )
    }
}

// ── Combined metrics card (2x2 grid) ─────────────────────────────────────────────

@Composable
private fun SpeedMetricsCard(
    state: SpeedTestUiState,
    onInfoClick: (String) -> Unit = {},
) {
    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Row 1: Download + Upload
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.speed_test_download),
                    value =
                        stringResource(
                            R.string.value_with_unit_text,
                            formatDecimal(state.downloadMbps, 1),
                            stringResource(R.string.unit_mbps),
                        ),
                    valueColor = accent,
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("download") },
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_upload),
                    value =
                        stringResource(
                            R.string.value_with_unit_text,
                            formatDecimal(state.uploadMbps, 1),
                            stringResource(R.string.unit_mbps),
                        ),
                    valueColor = accent,
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("upload") },
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            // Row 2: Ping + Jitter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.speed_test_ping),
                    value =
                        if (state.pingMs > 0) {
                            stringResource(
                                R.string.value_with_unit_int,
                                state.pingMs,
                                stringResource(R.string.unit_ms),
                            )
                        } else {
                            stringResource(R.string.placeholder_dash)
                        },
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("ping") },
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_jitter),
                    value =
                        if (state.jitterMs != null) {
                            stringResource(
                                R.string.value_with_unit_int,
                                state.jitterMs,
                                stringResource(R.string.unit_ms),
                            )
                        } else {
                            stringResource(R.string.placeholder_dash)
                        },
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("jitter") },
                )
            }
        }
    }
}

// ── Dialogs ──────────────────────────────────────────────────────────────────────

@Composable
private fun CellularDataWarningDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(text = stringResource(R.string.speed_test_title)) },
        text = { Text(text = stringResource(R.string.speed_test_cellular_warning)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.speed_test_cellular_proceed))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.speed_test_cellular_cancel))
            }
        },
    )
}

// ── Error & empty states ─────────────────────────────────────────────────────────

@Composable
private fun SpeedTestFailureCard(error: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        elevation = runcheckCardElevation(),
    ) {
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(MaterialTheme.spacing.base),
        )
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Text(
            text = stringResource(R.string.speed_test_no_history),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(MaterialTheme.spacing.base),
        )
    }
}

// ── Latest result card ───────────────────────────────────────────────────────────

@Composable
private fun LatestResultCard(result: SpeedTestResult) {
    val dateLabel = rememberTimestampLabel(result.timestamp)
    val accent = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(stringResource(R.string.speed_test_last_result))
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConnectionTypeBadge(result = result)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.speed_test_download),
                    value =
                        stringResource(
                            R.string.value_with_unit_text,
                            formatDecimal(result.downloadMbps, 1),
                            stringResource(R.string.unit_mbps),
                        ),
                    valueColor = accent,
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_upload),
                    value =
                        stringResource(
                            R.string.value_with_unit_text,
                            formatDecimal(result.uploadMbps, 1),
                            stringResource(R.string.unit_mbps),
                        ),
                    valueColor = accent,
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.speed_test_ping),
                    value = formatPingValue(result.pingMs),
                    modifier = Modifier.weight(1f),
                )
            }

            result.serverName?.let { server ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                MetricPill(
                    label = stringResource(R.string.speed_test_server),
                    value = server,
                )
            }
        }
    }
}

// ── History section ──────────────────────────────────────────────────────────────

@Composable
private fun HistorySection(results: List<SpeedTestResult>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        SectionHeader(stringResource(R.string.speed_test_history))

        results.forEach { result ->
            HistoryResultItem(result = result)
        }
    }
}

@Composable
private fun HistoryResultItem(result: SpeedTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ConnectionTypeIcon(
                        connectionType = result.connectionType,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = connectionTypeShortLabel(result),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = rememberTimestampLabel(result.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDecimal(result.downloadMbps, 0),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MaterialTheme.numericFontFamily,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.speed_test_download),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDecimal(result.uploadMbps, 0),
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MaterialTheme.numericFontFamily,
                            ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.speed_test_upload),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text =
                            if (result.pingMs >
                                0
                            ) {
                                result.pingMs.toString()
                            } else {
                                stringResource(R.string.placeholder_dash)
                            },
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontFamily = MaterialTheme.numericFontFamily,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.speed_test_ping),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Connection type helpers ──────────────────────────────────────────────────────

@Composable
private fun ConnectionTypeBadge(result: SpeedTestResult) {
    val label = connectionTypeShortLabel(result)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ConnectionTypeIcon(
            connectionType = result.connectionType,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        result.signalDbm?.let { dbm ->
            Text(
                text = stringResource(R.string.value_with_unit_int, dbm, stringResource(R.string.unit_dbm)),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontFamily = MaterialTheme.numericFontFamily,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ConnectionTypeIcon(
    connectionType: ConnectionType,
    modifier: Modifier = Modifier,
) {
    val icon =
        when (connectionType) {
            ConnectionType.WIFI -> Icons.Outlined.Wifi
            ConnectionType.CELLULAR -> Icons.Outlined.SignalCellularAlt
            ConnectionType.VPN -> Icons.Outlined.Wifi
            ConnectionType.NONE -> return
        }
    val description =
        when (connectionType) {
            ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
            ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
            ConnectionType.VPN -> stringResource(R.string.connection_vpn)
            ConnectionType.NONE -> return
        }
    Icon(
        imageVector = icon,
        contentDescription = description,
        modifier = modifier,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun connectionTypeShortLabel(result: SpeedTestResult): String {
    val base =
        when (result.connectionType) {
            ConnectionType.WIFI -> stringResource(R.string.connection_wifi)
            ConnectionType.CELLULAR -> stringResource(R.string.connection_cellular)
            ConnectionType.VPN -> stringResource(R.string.connection_vpn)
            ConnectionType.NONE -> stringResource(R.string.connection_none)
        }
    val subtype = result.networkSubtype
    return if (!subtype.isNullOrBlank()) stringResource(R.string.speed_test_connection_detail, base, subtype) else base
}

// ── Helpers ──────────────────────────────────────────────────────────────────────

@Composable
private fun rememberTimestampLabel(timestamp: Long): String = rememberFormattedDateTime(timestamp, "MMMdhm")

@Composable
private fun heroInstructionText(
    hasConnection: Boolean,
    phase: SpeedTestPhase,
): String =
    when {
        !hasConnection -> stringResource(R.string.speed_test_no_connection_hint)
        phase == SpeedTestPhase.Idle -> stringResource(R.string.speed_test_tap_ring_start)
        phase == SpeedTestPhase.Ping -> stringResource(R.string.speed_test_phase_ping)
        phase == SpeedTestPhase.Download -> stringResource(R.string.speed_test_phase_download)
        phase == SpeedTestPhase.Upload -> stringResource(R.string.speed_test_phase_upload)
        phase == SpeedTestPhase.Completed -> stringResource(R.string.speed_test_tap_ring_restart)
        phase is SpeedTestPhase.Failed -> stringResource(R.string.speed_test_tap_ring_retry)
        else -> ""
    }

@Composable
private fun formatPingValue(pingMs: Int): String =
    if (pingMs > 0) {
        stringResource(
            R.string.value_with_unit_int,
            pingMs,
            stringResource(R.string.unit_ms),
        )
    } else {
        stringResource(R.string.placeholder_dash)
    }

// ── Preview ──────────────────────────────────────────────────────────────────────

@PreviewsRuncheckThemes
@Composable
private fun SpeedTestContentPreview() {
    RuncheckTheme {
        SpeedTestContent(
            networkState =
                NetworkState(
                    connectionType = ConnectionType.WIFI,
                    signalDbm = -54,
                    signalQuality = com.runcheck.domain.model.SignalQuality.EXCELLENT,
                    wifiSsid = "Lab 6E",
                    latencyMs = 16,
                ),
            speedTestState =
                SpeedTestUiState(
                    phase = SpeedTestPhase.Completed,
                    downloadMbps = 542.7,
                    uploadMbps = 83.4,
                    pingMs = 17,
                    jitterMs = 3,
                    lastResult =
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
                            signalDbm = -54,
                        ),
                    recentResults =
                        listOf(
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
                                signalDbm = -54,
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
                                signalDbm = -57,
                            ),
                        ),
                ),
            onStartSpeedTest = {},
            onStopSpeedTest = {},
            onConfirmCellular = {},
            onDismissCellular = {},
        )
    }
}
