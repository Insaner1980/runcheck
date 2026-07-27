package com.runcheck.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.ui.common.LifecycleStartStopEffect
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.RuncheckEmptyState
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.RuncheckSingleChoiceSelector
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightRow
import com.runcheck.ui.home.insights.resolveInsightNavigationAction
import com.runcheck.ui.theme.spacing

@Composable
fun InsightsScreen(
    navigationHandlers: InsightNavigationHandlers,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFilter by rememberSaveable { mutableStateOf(InsightFilter.ALL) }
    val loadingDescription = stringResource(R.string.a11y_loading)

    LifecycleStartStopEffect(
        onStart = viewModel::onScreenVisible,
        onStop = viewModel::onScreenHidden,
    )

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTopBar(title = stringResource(R.string.insights_screen_title))

        when (val state = uiState) {
            InsightsUiState.Loading -> {
                ContentContainer(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        RuncheckProgressSpinner(contentDescription = loadingDescription)
                    }
                }
            }

            is InsightsUiState.Error -> {
                ContentContainer(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(MaterialTheme.spacing.lg),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.message.resolve(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is InsightsUiState.Success -> {
                val filteredInsights = selectedFilter.applyTo(state.insights)
                ContentContainer {
                    Column(
                        modifier =
                            Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

                        SectionHeader(text = stringResource(R.string.insights_filter_title))
                        RuncheckSingleChoiceSelector(
                            options = InsightFilter.entries,
                            selected = selectedFilter,
                            labelFor = { filter ->
                                stringResource(
                                    when (filter) {
                                        InsightFilter.ALL -> R.string.insights_filter_all
                                        InsightFilter.IMPORTANT -> R.string.insights_filter_important
                                    },
                                )
                            },
                            onSelect = { selectedFilter = it },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Text(
                            text =
                                pluralStringResource(
                                    id = R.plurals.insights_screen_count,
                                    count = filteredInsights.size,
                                    filteredInsights.size,
                                ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (filteredInsights.isEmpty()) {
                            RuncheckEmptyState(
                                title = stringResource(R.string.insights_empty_title),
                                message = stringResource(R.string.insights_screen_empty),
                            )
                        } else {
                            filteredInsights.forEach { insight ->
                                val navigationAction =
                                    resolveInsightNavigationAction(
                                        insight = insight,
                                        isPro = state.isPro,
                                        navigationHandlers = navigationHandlers,
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
