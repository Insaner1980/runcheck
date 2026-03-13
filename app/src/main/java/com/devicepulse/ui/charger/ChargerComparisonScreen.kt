package com.devicepulse.ui.charger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devicepulse.R
import com.devicepulse.domain.model.ChargerSummary
import com.devicepulse.ui.common.formatLocalizedDateTime
import com.devicepulse.ui.components.DetailTopBar
import com.devicepulse.ui.components.ProFeatureLockedState
import com.devicepulse.ui.theme.spacing

@Composable
fun ChargerComparisonScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    viewModel: ChargerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
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
                        Text(stringResource(R.string.error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            is ChargerUiState.Success -> {
                ChargerContent(
                    state = state,
                    onBack = onBack,
                    onAddClick = { showAddDialog = true },
                    onDeleteCharger = { viewModel.deleteCharger(it) }
                )
            }
            ChargerUiState.Locked -> {
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
}

@Composable
private fun ChargerContent(
    state: ChargerUiState.Success,
    onBack: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteCharger: (Long) -> Unit
) {
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
        if (state.chargers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.charger_no_chargers),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
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

                items(
                    items = state.chargers,
                    key = { it.chargerId }
                ) { charger ->
                    ChargerCard(
                        charger = charger,
                        onDelete = { onDeleteCharger(charger.chargerId) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
                }
            }
        }
    }
}

@Composable
private fun ChargerCard(
    charger: ChargerSummary,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = charger.chargerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.charger_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = pluralStringResource(
                    R.plurals.charger_sessions,
                    charger.sessionCount,
                    charger.sessionCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            charger.avgChargingSpeedMa?.let { speed ->
                Text(
                    text = stringResource(R.string.charger_avg_speed, speed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            charger.lastUsed?.let { timestamp ->
                val formatted = remember(timestamp) {
                    formatLocalizedDateTime(timestamp, "yMMMdHm")
                }
                Text(
                    text = stringResource(R.string.charger_last_used, formatted),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
