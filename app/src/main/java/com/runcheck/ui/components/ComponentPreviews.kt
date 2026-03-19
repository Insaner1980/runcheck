package com.runcheck.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.RuncheckTheme
import com.runcheck.ui.theme.spacing

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun ComponentStackPreview() {
    RuncheckTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = androidx.compose.material3.MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(
                androidx.compose.material3.MaterialTheme.spacing.base
            )
        ) {
            PrimaryTopBar(title = "Runcheck")
            DetailTopBar(title = "Battery", onBack = {})
            GridCard(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Battery",
                subtitle = "Healthy",
                onClick = {},
                subtitleColor = AccentTeal
            )
            ListRow(
                label = "Speed Test",
                icon = Icons.Outlined.BatteryChargingFull,
                onClick = {}
            )
        }
    }
}
