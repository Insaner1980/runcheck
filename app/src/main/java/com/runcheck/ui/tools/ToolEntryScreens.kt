package com.runcheck.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.components.ExpressiveDetailScaffold
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.RuncheckLoadingIndicator
import com.runcheck.ui.navigation.ProtectedFeatureAccessState
import com.runcheck.ui.navigation.protectedFeatureAccessState
import com.runcheck.ui.theme.spacing

@Composable
fun WeeklyReportEntryScreen(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.a11y_loading)
    ExpressiveDetailScaffold(
        title = stringResource(R.string.weekly_report_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (protectedFeatureAccessState(proStatusReady, hasProAccess)) {
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS -> {
                CenteredToolContent {
                    RuncheckLoadingIndicator(contentDescription = loadingDescription)
                }
            }

            ProtectedFeatureAccessState.LOCKED -> {
                ProFeatureLockedState(
                    title = stringResource(R.string.weekly_report_title),
                    message =
                        stringResource(
                            R.string.pro_feature_locked_message,
                            stringResource(R.string.weekly_report_title),
                        ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro,
                )
            }

            ProtectedFeatureAccessState.AVAILABLE -> {
                CenteredToolContent {
                    Text(
                        text = stringResource(R.string.weekly_report_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun ExportEntryScreen(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    availableContent: @Composable () -> Unit,
) {
    val loadingDescription = stringResource(R.string.a11y_loading)
    val accessState = protectedFeatureAccessState(proStatusReady, hasProAccess)
    if (accessState == ProtectedFeatureAccessState.AVAILABLE) {
        availableContent()
        return
    }
    ExpressiveDetailScaffold(
        title = stringResource(R.string.export_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (accessState) {
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS -> {
                CenteredToolContent {
                    RuncheckLoadingIndicator(contentDescription = loadingDescription)
                }
            }

            ProtectedFeatureAccessState.LOCKED -> {
                ProFeatureLockedState(
                    title = stringResource(R.string.export_title),
                    message =
                        stringResource(
                            R.string.pro_feature_locked_message,
                            stringResource(R.string.pro_feature_csv_export),
                        ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro,
                )
            }

            ProtectedFeatureAccessState.AVAILABLE -> {
                Unit
            }
        }
    }
}

@Composable
fun CleanupEntryScreen(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    availableContent: @Composable () -> Unit,
) {
    val accessState = protectedFeatureAccessState(proStatusReady, hasProAccess)
    when (accessState) {
        ProtectedFeatureAccessState.AVAILABLE -> {
            availableContent()
        }

        ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS,
        ProtectedFeatureAccessState.LOCKED,
        -> {
            val loadingDescription = stringResource(R.string.a11y_loading)
            ExpressiveDetailScaffold(
                title = stringResource(R.string.storage_cleanup_tools),
                onBack = onBack,
                modifier = modifier,
            ) {
                if (accessState == ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS) {
                    CenteredToolContent {
                        RuncheckLoadingIndicator(contentDescription = loadingDescription)
                    }
                } else {
                    ProFeatureLockedState(
                        title = stringResource(R.string.storage_cleanup_tools),
                        message =
                            stringResource(
                                R.string.pro_feature_locked_message,
                                stringResource(R.string.storage_cleanup_tools),
                            ),
                        actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                        onAction = onUpgradeToPro,
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredToolContent(content: @Composable () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(vertical = MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}
