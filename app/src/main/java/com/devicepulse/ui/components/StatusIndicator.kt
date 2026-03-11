package com.devicepulse.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devicepulse.R
import com.devicepulse.domain.model.HealthStatus
import com.devicepulse.ui.theme.statusColors

@Composable
fun StatusIndicator(
    status: HealthStatus,
    modifier: Modifier = Modifier,
    customLabel: String? = null
) {
    val statusColors = MaterialTheme.statusColors

    val color = when (status) {
        HealthStatus.HEALTHY -> statusColors.healthy
        HealthStatus.FAIR -> statusColors.fair
        HealthStatus.POOR -> statusColors.poor
        HealthStatus.CRITICAL -> statusColors.critical
    }

    val icon = when (status) {
        HealthStatus.HEALTHY -> Icons.Filled.CheckCircle
        HealthStatus.FAIR -> Icons.Filled.Info
        HealthStatus.POOR -> Icons.Filled.Warning
        HealthStatus.CRITICAL -> Icons.Filled.Error
    }

    val label = customLabel ?: when (status) {
        HealthStatus.HEALTHY -> stringResource(R.string.status_healthy)
        HealthStatus.FAIR -> stringResource(R.string.status_fair)
        HealthStatus.POOR -> stringResource(R.string.status_poor)
        HealthStatus.CRITICAL -> stringResource(R.string.status_critical)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}
