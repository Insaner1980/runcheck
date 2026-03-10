package com.devicepulse.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.devicepulse.R

@Composable
fun MoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.more_thermal)) },
            onClick = {
                onNavigate(Screen.Thermal.route)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.Thermostat, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.more_storage)) },
            onClick = {
                onNavigate(Screen.Storage.route)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.SdStorage, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.more_charger)) },
            onClick = {
                onNavigate(Screen.Charger.route)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.BatteryChargingFull, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.more_app_usage)) },
            onClick = {
                onNavigate(Screen.AppUsage.route)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.DataUsage, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.more_settings)) },
            onClick = {
                onNavigate(Screen.Settings.route)
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.Settings, contentDescription = null)
            }
        )
    }
}
