package com.runcheck.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.home.insights.InsightRow
import com.runcheck.ui.home.insights.resolveInsightNavigationAction
import com.runcheck.ui.theme.spacing

@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    onNavigateToBattery: () -> Unit,
    onNavigateToNetwork: () -> Unit,
    onNavigateToThermal: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToCharger: () -> Unit,
    onNavigateToAppUsage: () -> Unit,
    onNavigateToProUpgrade: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        DetailTopBar(
            title = stringResource(R.string.insights_screen_title),
            onBack = onBack,
        )

        when (val state = uiState) {
            InsightsUiState.Loading -> {
                ContentContainer(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is InsightsUiState.Error -> {
                ContentContainer(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is InsightsUiState.Success -> {
                ContentContainer {
                    Column(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 24.dp)
                                .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                        Text(
                            text =
                                pluralStringResource(
                                    id = R.plurals.insights_screen_count,
                                    count = state.insights.size,
                                    state.insights.size,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (state.insights.isEmpty()) {
                            Text(
                                text = stringResource(R.string.insights_screen_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            state.insights.forEach { insight ->
                                val navigationAction =
                                    resolveInsightNavigationAction(
                                        insight = insight,
                                        isPro = state.isPro,
                                        onNavigateToBattery = onNavigateToBattery,
                                        onNavigateToNetwork = onNavigateToNetwork,
                                        onNavigateToThermal = onNavigateToThermal,
                                        onNavigateToStorage = onNavigateToStorage,
                                        onNavigateToCharger = onNavigateToCharger,
                                        onNavigateToAppUsage = onNavigateToAppUsage,
                                        onNavigateToProUpgrade = onNavigateToProUpgrade,
                                    )

                                InsightRow(
                                    insight = insight,
                                    onClick = navigationAction.onClick,
                                    onDismiss = { viewModel.dismissInsight(insight.id) },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
                    }
                }
            }
        }
    }
}
