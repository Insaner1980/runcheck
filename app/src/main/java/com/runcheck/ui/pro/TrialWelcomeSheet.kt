package com.runcheck.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.pro.ProFeature
import com.runcheck.ui.theme.BottomSheetShape
import com.runcheck.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialWelcomeSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = BottomSheetShape,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.lg)
                    .padding(bottom = MaterialTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.trial_welcome_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = stringResource(R.string.trial_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.lg))

            ProFeature.entries.forEach { feature ->
                WelcomeFeatureRow(label = welcomeFeatureLabel(feature))
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Button(
                onClick = onDismiss,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    text = stringResource(R.string.trial_welcome_start),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun welcomeFeatureLabel(feature: ProFeature): String =
    when (feature) {
        ProFeature.EXTENDED_HISTORY -> stringResource(R.string.pro_feature_extended_history)
        ProFeature.CHARGER_COMPARISON -> stringResource(R.string.pro_feature_charger_comparison)
        ProFeature.PER_APP_BATTERY -> stringResource(R.string.pro_feature_per_app_battery)
        ProFeature.WIDGETS -> stringResource(R.string.pro_feature_widgets)
        ProFeature.CSV_EXPORT -> stringResource(R.string.pro_feature_csv_export)
        ProFeature.THERMAL_LOGS -> stringResource(R.string.pro_feature_thermal_logs)
    }
