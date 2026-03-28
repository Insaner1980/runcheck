package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.runcheck.domain.model.Confidence
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ComponentStackPreview() {
    RuncheckTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(all = androidx.compose.material3.MaterialTheme.spacing.base),
            verticalArrangement =
                Arrangement.spacedBy(
                    androidx.compose.material3.MaterialTheme.spacing.base,
                ),
        ) {
            PrimaryTopBar(title = "Runcheck")
            DetailTopBar(title = "Battery", onBack = {})
            GridCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Battery",
                subtitle = "Healthy",
                onClick = {},
                subtitleColor = MaterialTheme.statusColors.healthy,
            )
            ListRow(
                label = "Speed Test",
                icon = Icons.Outlined.BatteryChargingFull,
                onClick = {},
            )
            ConfidenceBadge(confidence = Confidence.HIGH)
            ProgressRing(
                progress = 0.72f,
                modifier = Modifier.size(88.dp),
                contentDescription = "Preview progress ring",
            )
            ProFeatureLockedState(
                title = "Thermal Logs",
                message = "Unlock extended logs and history.",
                actionLabel = "Upgrade",
                onAction = {},
            )
        }
    }
}
