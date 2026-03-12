package com.devicepulse.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devicepulse.R
import com.devicepulse.ui.components.PrimaryTopBar
import com.devicepulse.ui.theme.spacing

@Composable
fun MoreScreen(
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val items = listOf(
        MoreDestination(stringResource(R.string.more_thermal), Icons.Outlined.Thermostat, onNavigateToThermal),
        MoreDestination(stringResource(R.string.more_storage), Icons.Outlined.Storage, onNavigateToStorage),
        MoreDestination(stringResource(R.string.more_charger), Icons.Outlined.BatteryChargingFull, onNavigateToCharger),
        MoreDestination(stringResource(R.string.more_app_usage), Icons.Outlined.DataUsage, onNavigateToAppUsage),
        MoreDestination(stringResource(R.string.more_settings), Icons.Outlined.Settings, onNavigateToSettings)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTopBar(title = stringResource(R.string.nav_more))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            }

            items(items) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    onClick = item.onClick,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.base),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }
}

private data class MoreDestination(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)
