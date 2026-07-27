package com.runcheck.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.RuncheckDetailScaffold
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.navigation.ProtectedFeatureAccessState
import com.runcheck.ui.navigation.protectedFeatureAccessState
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.weekly.WeeklyReportContent
import com.runcheck.ui.weekly.WeeklyReportUiState
import com.runcheck.ui.weekly.WeeklyReportViewModel

@Composable
fun WeeklyReportEntryScreen(
    proStatusReady: Boolean,
    hasProAccess: Boolean,
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyReportViewModel = hiltViewModel(),
) {
    val loadingDescription = stringResource(R.string.a11y_loading)
    val accessState = protectedFeatureAccessState(proStatusReady, hasProAccess)
    val reportState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(accessState) {
        if (accessState == ProtectedFeatureAccessState.AVAILABLE) {
            viewModel.load()
        }
    }
    RuncheckDetailScaffold(
        title = stringResource(R.string.weekly_report_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (accessState) {
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS -> {
                CenteredToolContent {
                    RuncheckProgressSpinner(contentDescription = loadingDescription)
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
                when (val state = reportState) {
                    WeeklyReportUiState.Loading -> {
                        CenteredToolContent {
                            RuncheckProgressSpinner(contentDescription = loadingDescription)
                        }
                    }

                    WeeklyReportUiState.Locked -> {
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

                    is WeeklyReportUiState.Error -> {
                        CenteredToolContent {
                            Text(
                                text = state.message.resolve(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    is WeeklyReportUiState.Success -> {
                        WeeklyReportContent(report = state.report)
                    }
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
    RuncheckDetailScaffold(
        title = stringResource(R.string.export_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (accessState) {
            ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS -> {
                CenteredToolContent {
                    RuncheckProgressSpinner(contentDescription = loadingDescription)
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
            RuncheckDetailScaffold(
                title = stringResource(R.string.storage_cleanup_tools),
                onBack = onBack,
                modifier = modifier,
            ) {
                if (accessState == ProtectedFeatureAccessState.WAITING_FOR_PRO_STATUS) {
                    CenteredToolContent {
                        RuncheckProgressSpinner(contentDescription = loadingDescription)
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
