package com.runcheck.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.domain.model.HealthStatus
import com.runcheck.ui.theme.statusColors

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

    val label = customLabel ?: when (status) {
        HealthStatus.HEALTHY -> stringResource(R.string.status_healthy)
        HealthStatus.FAIR -> stringResource(R.string.status_fair)
        HealthStatus.POOR -> stringResource(R.string.status_poor)
        HealthStatus.CRITICAL -> stringResource(R.string.status_critical)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
