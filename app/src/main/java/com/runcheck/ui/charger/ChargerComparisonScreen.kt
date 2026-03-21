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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.ChargerSummary
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.rememberFormattedDateTime
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.theme.spacing
import kotlin.math.max

@Composable
fun ChargerComparisonScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChargerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDeleteCharger by remember { mutableStateOf<ChargerSummary?>(null) }

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
        when (val state = uiState) {
            is ChargerUiState.Loading -> {
                DetailTopBar(
                    title = stringResource(R.string.charger_title),
                    onBack = onBack
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is ChargerUiState.Error -> {
                DetailTopBar(
                    title = stringResource(R.string.charger_title),
                    onBack = onBack
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is ChargerUiState.Success -> {
                ChargerContent(
                    state = state,
                    onBack = onBack,
                    onAddClick = { showAddDialog = true },
                    onSelectCharger = { viewModel.selectCharger(it) },
                    onClearSelectedCharger = { viewModel.clearSelectedCharger() },
                    onDeleteRequest = { pendingDeleteCharger = it }
                )
            }

            ChargerUiState.Locked -> {
                LaunchedEffect(Unit) {
                    onUpgradeToPro()
                }
                DetailTopBar(
                    title = stringResource(R.string.charger_title),
                    onBack = onBack
                )
                ProFeatureLockedState(
                    title = stringResource(R.string.charger_title),
                    message = stringResource(
                        R.string.pro_feature_locked_message,
                        stringResource(R.string.charger_title)
                    ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro
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
            }
        )
    }

    pendingDeleteCharger?.let { charger ->
        DeleteChargerDialog(
            chargerName = charger.chargerName,
            onDismiss = { pendingDeleteCharger = null },
            onConfirm = {
                viewModel.deleteCharger(charger.chargerId)
                pendingDeleteCharger = null
            }
        )
    }
}

@Composable
private fun ChargerContent(
    state: ChargerUiState.Success,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onSelectCharger: (Long) -> Unit,
    onClearSelectedCharger: () -> Unit,
    onDeleteRequest: (ChargerSummary) -> Unit
) {
    val selectedCharger = state.chargers.firstOrNull { it.chargerId == state.selectedChargerId }

    Scaffold(
        topBar = {
            DetailTopBar(
                title = stringResource(R.string.charger_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.charger_add)
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            item {
                SelectedChargerCard(
                    chargerName = selectedCharger?.chargerName,
                    hasActiveSession = selectedCharger?.hasActiveSession == true,
                    onClearSelectedCharger = onClearSelectedCharger
                )
            }

            if (state.chargers.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                val chargersWithHistory = state.chargers.filter { it.avgPowerMw != null || it.avgChargingSpeedMa != null }
                if (chargersWithHistory.isNotEmpty()) {
                    item {
                        HistoricalComparisonCard(chargers = chargersWithHistory)
                    }
                }

                items(
                    items = state.chargers,
                    key = { it.chargerId }
                ) { charger ->
                    ChargerCard(
                        charger = charger,
                        isSelected = charger.chargerId == state.selectedChargerId,
                        onSelect = { onSelectCharger(charger.chargerId) },
                        onClearSelected = onClearSelectedCharger,
                        onDelete = { onDeleteRequest(charger) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

@Composable
private fun SelectedChargerCard(
    chargerName: String?,
    hasActiveSession: Boolean,
    onClearSelectedCharger: () -> Unit
) {
    InfoCardContainer {
        Text(
            text = stringResource(R.string.charger_selection_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = chargerName ?: stringResource(R.string.charger_selection_none),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = if (chargerName == null) {
                stringResource(R.string.charger_selection_hint)
            } else if (hasActiveSession) {
                stringResource(R.string.charger_selection_active)
            } else {
                stringResource(R.string.charger_selection_ready)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun EmptyStateCard() {
    InfoCardContainer {
        Text(
            text = stringResource(R.string.charger_no_chargers),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = stringResource(R.string.charger_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text = stringResource(R.string.charger_historical_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        sortedChargers.forEach { charger ->
            val comparisonValue = chargerComparisonValue(charger)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = charger.chargerName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = comparisonLabel(charger),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(comparisonValue.toFloat() / maxValue.toFloat())
                            .height(8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.extraLarge
                            )
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
    onClearSelected: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = charger.chargerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected || charger.hasActiveSession) {
                        Text(
                            text = if (charger.hasActiveSession) {
                                stringResource(R.string.charger_selected_active)
                            } else {
                                stringResource(R.string.charger_selected)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.charger_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = pluralStringResource(
                    R.plurals.charger_sessions,
                    charger.sessionCount,
                    charger.sessionCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))

            Text(
                text = latestResultLabel(charger),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            charger.lastUsed?.let { timestamp ->
                val formatted = rememberFormattedDateTime(timestamp, "yMMMdHm")
                Text(
                    text = stringResource(R.string.charger_last_test, formatted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            OutlinedButton(
                onClick = if (isSelected) onClearSelected else onSelect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isSelected) {
                        stringResource(R.string.charger_clear_selected)
                    } else {
                        stringResource(R.string.charger_select)
                    }
                )
            }
        }
    }
}

@Composable
private fun AddChargerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.charger_add)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.charger_add_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.charger_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteChargerDialog(
    chargerName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
        }
    )
}

@Composable
private fun InfoCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            content = content
        )
    }
}

private fun chargerComparisonValue(charger: ChargerSummary): Int =
    charger.avgPowerMw ?: charger.avgChargingSpeedMa ?: 0

@Composable
private fun comparisonLabel(charger: ChargerSummary): String =
    charger.avgPowerMw?.let { powerMw ->
        stringResource(R.string.charger_average_power, formatDecimal(powerMw / 1000f, 1))
    } ?: stringResource(
        R.string.charger_average_current,
        charger.avgChargingSpeedMa ?: 0
    )

@Composable
private fun latestResultLabel(charger: ChargerSummary): String =
    charger.latestPowerMw?.let { powerMw ->
        stringResource(R.string.charger_latest_power, formatDecimal(powerMw / 1000f, 1))
    } ?: charger.latestChargingSpeedMa?.let { currentMa ->
        stringResource(R.string.charger_latest_current, currentMa)
    } ?: stringResource(R.string.charger_no_completed_tests)
