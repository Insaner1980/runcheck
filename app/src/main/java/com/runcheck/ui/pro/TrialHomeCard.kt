package com.runcheck.ui.pro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.runcheck.R
import com.runcheck.pro.ProState
import com.runcheck.pro.ProStatus
import com.runcheck.pro.TrialManager
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.statusColors

@Composable
fun TrialHomeCard(
    proState: ProState,
    onNavigateToProUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (proState.status != ProStatus.TRIAL_ACTIVE) return

    val isUrgent = proState.trialDaysRemaining <= 1
    val accentColor =
        if (isUrgent) {
            MaterialTheme.statusColors.poor
        } else {
            MaterialTheme.colorScheme.primary
        }
    val progress = 1f - (proState.trialDaysRemaining.toFloat() / TrialManager.TRIAL_DURATION_DAYS)
    val progressDescription =
        stringResource(
            R.string.a11y_progress_percent,
            stringResource(R.string.trial_pro_trial_label),
            (progress * 100).toInt(),
        )

    Card(
        onClick = onNavigateToProUpgrade,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        if (isUrgent) {
                            stringResource(R.string.trial_ends_tomorrow)
                        } else {
                            stringResource(R.string.trial_days_remaining, proState.trialDaysRemaining)
                        },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )

                if (isUrgent) {
                    TextButton(onClick = onNavigateToProUpgrade) {
                        Text(
                            text = stringResource(R.string.trial_keep_pro),
                            color = accentColor,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .semantics {
                            contentDescription = progressDescription
                            progressBarRangeInfo =
                                androidx.compose.ui.semantics
                                    .ProgressBarRangeInfo(progress, 0f..1f)
                        },
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.trial_pro_trial_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PostExpirationUpgradeCard(
    formattedPrice: String?,
    onNavigateToProUpgrade: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onNavigateToProUpgrade,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text =
                    if (formattedPrice != null) {
                        stringResource(R.string.trial_expired_upgrade_with_price, formattedPrice)
                    } else {
                        stringResource(R.string.trial_expired_upgrade)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.trial_dismiss),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
