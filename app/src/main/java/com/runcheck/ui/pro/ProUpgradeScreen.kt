package com.runcheck.ui.pro

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.NoAccounts
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.pro.ProFeature
import com.runcheck.pro.ProStatus
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.theme.TextSecondary

@Composable
fun ProUpgradeScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProUpgradeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.pro_upgrade_title),
            onBack = onBack
        )

        if (uiState.purchaseCompleted) {
            PurchaseThankYouContent(
                onDismiss = {
                    viewModel.dismissThankYou()
                    onBack()
                }
            )
        } else if (uiState.proState.status == ProStatus.PRO_PURCHASED) {
            ProActiveContent()
        } else {
            ProUpgradeContent(
                uiState = uiState,
                onPurchase = {
                    (context as? Activity)?.let { viewModel.purchasePro(it) }
                }
            )
        }
    }
}

@Composable
private fun ProUpgradeContent(
    uiState: ProUpgradeUiState,
    onPurchase: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.pro_upgrade_headline),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.pro_upgrade_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        ProFeature.entries.forEach { feature ->
            FeatureRow(
                icon = featureIcon(feature),
                label = featureLabel(feature)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onPurchase,
            enabled = uiState.billingAvailable,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (uiState.formattedPrice != null) {
                    stringResource(R.string.pro_upgrade_buy_button, uiState.formattedPrice!!)
                } else {
                    stringResource(R.string.pro_upgrade_buy_button_no_price)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.pro_upgrade_one_time),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProActiveContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_pro_active),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.settings_pro_thank_you),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}

@Composable
private fun featureIcon(feature: ProFeature): ImageVector = when (feature) {
    ProFeature.EXTENDED_HISTORY -> Icons.Outlined.BarChart
    ProFeature.CHARGER_COMPARISON -> Icons.Outlined.BatteryChargingFull
    ProFeature.PER_APP_BATTERY -> Icons.Outlined.DataUsage
    ProFeature.WIDGETS -> Icons.Outlined.Widgets
    ProFeature.CSV_EXPORT -> Icons.Outlined.FileDownload
    ProFeature.THERMAL_LOGS -> Icons.Outlined.Thermostat
    ProFeature.AD_FREE -> Icons.Outlined.NoAccounts
}

@Composable
private fun featureLabel(feature: ProFeature): String = when (feature) {
    ProFeature.EXTENDED_HISTORY -> stringResource(R.string.pro_feature_extended_history)
    ProFeature.CHARGER_COMPARISON -> stringResource(R.string.pro_feature_charger_comparison)
    ProFeature.PER_APP_BATTERY -> stringResource(R.string.pro_feature_per_app_battery)
    ProFeature.WIDGETS -> stringResource(R.string.pro_feature_widgets)
    ProFeature.CSV_EXPORT -> stringResource(R.string.pro_feature_csv_export)
    ProFeature.THERMAL_LOGS -> stringResource(R.string.pro_feature_thermal_logs)
    ProFeature.AD_FREE -> stringResource(R.string.pro_feature_ad_free)
}
