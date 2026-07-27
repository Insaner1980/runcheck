package com.runcheck.ui.charger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.ChargerSummary
import com.runcheck.ui.common.LifecycleStartStopEffect
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.RuncheckDetailScaffold
import com.runcheck.ui.components.RuncheckEmptyState
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.StatusPill
import com.runcheck.ui.components.StatusTone
import com.runcheck.ui.theme.RuncheckPillShape
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.uiTokens
import kotlin.math.max

@Composable
fun ChargerComparisonScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteChargerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingDeleteChargerName by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleStartStopEffect(
        onStart = viewModel::startObserving,
        onStop = viewModel::stopObserving,
    )

    RuncheckDetailScaffold(
        title = stringResource(R.string.charger_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (val state = uiState) {
            is ChargerUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RuncheckProgressSpinner(
                        contentDescription = stringResource(R.string.a11y_loading),
                    )
                }
            }

            is ChargerUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message.resolve())
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is ChargerUiState.Success -> {
                ChargerContent(
                    state = state,
                    onAddClick = { showAddDialog = true },
                    onSelectCharger = { viewModel.selectCharger(it) },
                    onClearSelectedCharger = { viewModel.clearSelectedCharger() },
                    onDeleteRequest = {
                        pendingDeleteChargerId = it.chargerId
                        pendingDeleteChargerName = it.chargerName
                    },
                )
            }

            ChargerUiState.Locked -> {
                val currentOnUpgradeToPro by rememberUpdatedState(onUpgradeToPro)
                LaunchedEffect(Unit) {
                    currentOnUpgradeToPro()
                }
                ProFeatureLockedState(
                    title = stringResource(R.string.charger_title),
                    message =
                        stringResource(
                            R.string.pro_feature_locked_message,
                            stringResource(R.string.charger_title),
                        ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro,
                )
            }
        }
    }

    if (showAddDialog && uiState is ChargerUiState.Success) {
        AddChargerDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addCharger(name)
                showAddDialog = false
            },
        )
    }

    pendingDeleteChargerId?.let { chargerId ->
        DeleteChargerDialog(
            chargerName = pendingDeleteChargerName.orEmpty(),
            onDismiss = {
                pendingDeleteChargerId = null
            },
            onConfirm = {
                viewModel.deleteCharger(chargerId)
                pendingDeleteChargerId = null
            },
        )
    }
}

@Composable
private fun ChargerContent(
    state: ChargerUiState.Success,
    onAddClick: () -> Unit,
    onSelectCharger: (Long) -> Unit,
    onClearSelectedCharger: () -> Unit,
    onDeleteRequest: (ChargerSummary) -> Unit,
) {
    val selectedCharger = state.chargers.firstOrNull { it.chargerId == state.selectedChargerId }
    val tokens = MaterialTheme.uiTokens

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
        ) {
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            if (state.chargers.isNotEmpty()) {
                item {
                    SelectedChargerCard(
                        chargerName = selectedCharger?.chargerName,
                        hasActiveSession = selectedCharger?.hasActiveSession == true,
                        onClearSelectedCharger = onClearSelectedCharger,
                    )
                }
            }

            if (state.chargers.isEmpty()) {
                item {
                    EmptyStateCard(onAddClick = onAddClick)
                }
            } else {
                val chargersWithHistory =
                    state.chargers.filter {
                        it.avgPowerMw != null || it.avgChargingSpeedMa != null
                    }
                if (chargersWithHistory.isNotEmpty()) {
                    item {
                        HistoricalComparisonCard(chargers = chargersWithHistory)
                    }
                }

                items(
                    items = state.chargers,
                    key = { it.chargerId },
                ) { charger ->
                    val onSelect =
                        remember(charger.chargerId) {
                            { onSelectCharger(charger.chargerId) }
                        }
                    val onDelete =
                        remember(charger.chargerId) {
                            { onDeleteRequest(charger) }
                        }
                    ChargerCard(
                        charger = charger,
                        isSelected = charger.chargerId == state.selectedChargerId,
                        onSelect = onSelect,
                        onClearSelection = onClearSelectedCharger,
                        onDelete = onDelete,
                    )
                }
            }

            item {
                Spacer(
                    modifier =
                        Modifier.height(
                            tokens.primaryButtonHeight + MaterialTheme.spacing.xl,
                        ),
                )
            }
        }

        if (state.chargers.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAddClick,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(MaterialTheme.spacing.base),
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(R.string.charger_add)) },
            )
        }
    }
}

@Composable
private fun SelectedChargerCard(
    chargerName: String?,
    hasActiveSession: Boolean,
    onClearSelectedCharger: () -> Unit,
) {
    InfoCardContainer {
        Text(
            text = stringResource(R.string.charger_selection_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = chargerName ?: stringResource(R.string.charger_selection_none),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text =
                if (chargerName == null) {
                    stringResource(R.string.charger_selection_hint)
                } else if (hasActiveSession) {
                    stringResource(R.string.charger_selection_active)
                } else {
                    stringResource(R.string.charger_selection_ready)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (chargerName != null) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            TextButton(onClick = onClearSelectedCharger) {
                Text(stringResource(R.string.charger_clear_selected))
            }
        }
    }
}

@Composable
private fun EmptyStateCard(onAddClick: () -> Unit) {
    InfoCardContainer {
        RuncheckEmptyState(
            title = stringResource(R.string.charger_no_chargers),
            message = stringResource(R.string.charger_empty_body),
            illustration = { ChargerEmptyIllustration() },
        )
        Button(onClick = onAddClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.charger_add))
        }
    }
}

@Composable
private fun ChargerEmptyIllustration() {
    Box(
        modifier = Modifier.size(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.BatteryChargingFull,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp),
        )
    }
}

@Composable
private fun HistoricalComparisonCard(chargers: List<ChargerSummary>) {
    val values = remember(chargers) { chargers.map(::chargerComparisonValue) }
    val maxValue = remember(values) { max(1, values.maxOrNull() ?: 1) }
    val sortedChargers = remember(chargers) { chargers.sortedByDescending(::chargerComparisonValue) }

    InfoCardContainer {
        Text(
            text = stringResource(R.string.charger_historical_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = stringResource(R.string.charger_historical_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        sortedChargers.forEach { charger ->
            val comparisonValue = chargerComparisonValue(charger)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = charger.chargerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = comparisonLabel(charger),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                shape = RuncheckPillShape,
                            ),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(comparisonValue.toFloat() / maxValue.toFloat())
                                .height(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RuncheckPillShape,
                                ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }
    }
}

@Composable
private fun ChargerCard(
    charger: ChargerSummary,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
) {
    // Pre-compute conditional values to reduce nesting complexity
    val selectedLabel: String? =
        when {
            charger.hasActiveSession -> stringResource(R.string.charger_selected_active)
            isSelected -> stringResource(R.string.charger_selected)
            else -> null
        }

    val buttonText =
        if (isSelected) {
            stringResource(R.string.charger_clear_selected)
        } else {
            stringResource(R.string.charger_select)
        }
    val buttonAction = if (isSelected) onClearSelection else onSelect

    val lastUsedText =
        charger.lastUsed?.let { timestamp ->
            val formatted = rememberFormattedDateTime(timestamp, "yMMMdHm")
            stringResource(R.string.charger_last_test, formatted)
        }

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
                    .padding(MaterialTheme.spacing.base),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = charger.chargerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    selectedLabel?.let {
                        StatusPill(
                            label = it,
                            tone =
                                if (charger.hasActiveSession) {
                                    StatusTone.HEALTHY
                                } else {
                                    StatusTone.NEUTRAL
                                },
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.charger_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text =
                    pluralStringResource(
                        R.plurals.charger_sessions,
                        charger.sessionCount,
                        charger.sessionCount,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = latestResultLabel(charger),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            lastUsedText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            OutlinedButton(
                onClick = buttonAction,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = buttonText)
            }
        }
    }
}

@Composable
private fun AddChargerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.charger_add)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.charger_add_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.charger_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun DeleteChargerDialog(
    chargerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.charger_delete_confirm_title)) },
        text = { Text(stringResource(R.string.charger_delete_confirm_message, chargerName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.charger_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
private fun InfoCardContainer(content: @Composable ColumnScope.() -> Unit) {
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
                    .padding(MaterialTheme.spacing.base),
            content = content,
        )
    }
}

private fun chargerComparisonValue(charger: ChargerSummary): Int = charger.avgPowerMw ?: charger.avgChargingSpeedMa ?: 0

@Composable
private fun comparisonLabel(charger: ChargerSummary): String =
    charger.avgPowerMw?.let { powerMw ->
        stringResource(R.string.charger_average_power, formatDecimal(powerMw / 1000f, 1))
    } ?: stringResource(
        R.string.charger_average_current,
        charger.avgChargingSpeedMa ?: 0,
    )

@Composable
private fun latestResultLabel(charger: ChargerSummary): String =
    charger.latestPowerMw?.let { powerMw ->
        stringResource(R.string.charger_latest_power, formatDecimal(powerMw / 1000f, 1))
    } ?: charger.latestChargingSpeedMa?.let { currentMa ->
        stringResource(R.string.charger_latest_current, currentMa)
    } ?: stringResource(R.string.charger_no_completed_tests)
