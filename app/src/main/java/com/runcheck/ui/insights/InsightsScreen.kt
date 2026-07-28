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
import androidx.compose.runtime.LaunchedEffect
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
import com.runcheck.ui.components.EmptyStateIllustration
import com.runcheck.ui.components.PrimaryTopBar
import com.runcheck.ui.components.RuncheckProgressSpinner
import com.runcheck.ui.components.RuncheckSingleChoiceSelector
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.home.insights.InsightNavigationHandlers
import com.runcheck.ui.home.insights.InsightRow
import com.runcheck.ui.home.insights.resolveInsightNavigationAction
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.weekly.WeeklyReportSummaryContent
import com.runcheck.ui.weekly.WeeklyReportUiState
import com.runcheck.ui.weekly.WeeklyReportViewModel

@Composable
fun InsightsScreen(
    navigationHandlers: InsightNavigationHandlers,
    onNavigateHome: () -> Unit,
    onNavigateToWeeklyReport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
    weeklyReportViewModel: WeeklyReportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weeklyReportState by weeklyReportViewModel.uiState.collectAsStateWithLifecycle()
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
                LaunchedEffect(state.isPro) {
                    if (state.isPro) {
                        weeklyReportViewModel.load()
                    }
                }
                InsightsContent(
                    state = state,
                    weeklyReportState =
                        if (state.isPro) {
                            weeklyReportState
                        } else {
                            WeeklyReportUiState.Locked
                        },
                    navigationHandlers = navigationHandlers,
                    onDismissInsight = viewModel::dismissInsight,
                    onNavigateHome = onNavigateHome,
                    onNavigateToWeeklyReport = onNavigateToWeeklyReport,
                    selectedFilter = selectedFilter,
                    onSelectFilter = { selectedFilter = it },
                )
            }
        }
    }
}

@Composable
internal fun InsightsContent(
    state: InsightsUiState.Success,
    weeklyReportState: WeeklyReportUiState,
    navigationHandlers: InsightNavigationHandlers,
    onDismissInsight: (Long) -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToWeeklyReport: () -> Unit,
    selectedFilter: InsightFilter,
    onSelectFilter: (InsightFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = groupInsights(state.insights, selectedFilter)
    val filteredCount = sections.needsAttention.size + sections.other.size

    ContentContainer(modifier = modifier) {
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
                onSelect = onSelectFilter,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text =
                    pluralStringResource(
                        id = R.plurals.insights_screen_count,
                        count = filteredCount,
                        filteredCount,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (filteredCount == 0) {
                EmptyStateIllustration(
                    title = stringResource(R.string.insights_empty_title),
                    message =
                        stringResource(
                            if (state.insights.isEmpty()) {
                                R.string.insights_screen_empty
                            } else {
                                R.string.insights_filter_empty
                            },
                        ),
                    actionLabel = stringResource(R.string.insights_empty_home_action),
                    onAction = onNavigateHome,
                )
            } else {
                InsightSection(
                    title = stringResource(R.string.insights_needs_attention),
                    insights = sections.needsAttention,
                    state = state,
                    navigationHandlers = navigationHandlers,
                    onDismissInsight = onDismissInsight,
                )
            }

            SectionHeader(text = stringResource(R.string.insights_this_week))
            WeeklyReportSummaryContent(
                state = weeklyReportState,
                onOpenReport = onNavigateToWeeklyReport,
            )

            InsightSection(
                title = stringResource(R.string.insights_other),
                insights = sections.other,
                state = state,
                navigationHandlers = navigationHandlers,
                onDismissInsight = onDismissInsight,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun InsightSection(
    title: String,
    insights: List<com.runcheck.domain.insights.model.Insight>,
    state: InsightsUiState.Success,
    navigationHandlers: InsightNavigationHandlers,
    onDismissInsight: (Long) -> Unit,
) {
    if (insights.isEmpty()) return

    SectionHeader(text = title, count = insights.size)
    insights.forEach { insight ->
        val navigationAction =
            resolveInsightNavigationAction(
                insight = insight,
                isPro = state.isPro,
                navigationHandlers = navigationHandlers,
            )

        InsightRow(
            insight = insight,
            onClick = navigationAction.onClick,
            onDismiss = { onDismissInsight(insight.id) },
        )
    }
}
