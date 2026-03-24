package com.runcheck.ui.storage

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.R
import com.runcheck.domain.model.HistoryPeriod
import com.runcheck.domain.model.MediaCategory
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.TrashInfo
import com.runcheck.domain.model.StorageState
import com.runcheck.ui.ads.DetailScreenAdBanner
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.storage.MediaDeleteRequestResult
import com.runcheck.ui.storage.cleanup.categoryColor
import com.runcheck.ui.components.info.InfoBottomSheet
import com.runcheck.ui.components.info.InfoCard
import com.runcheck.ui.components.info.InfoCardCatalog
import com.runcheck.ui.chart.MAX_STORAGE_HISTORY_POINTS
import com.runcheck.ui.chart.StorageHistoryMetric
import com.runcheck.ui.chart.buildStorageHistoryChartModel
import com.runcheck.ui.chart.formatChartTooltip
import com.runcheck.ui.chart.historyPeriodLabel
import com.runcheck.ui.chart.storageHistoryMetricLabel
import com.runcheck.ui.common.UiText
import com.runcheck.ui.common.formatDecimal
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.ActionCard
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ExpandableChartContainer
import com.runcheck.ui.components.ListRow
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.ProFeatureCalloutCard
import com.runcheck.ui.components.MetricRow
import com.runcheck.ui.components.ProgressRing
import com.runcheck.ui.components.PullToRefreshWrapper
import com.runcheck.ui.components.SectionHeader
import com.runcheck.ui.components.SegmentData
import com.runcheck.ui.components.SegmentedBar
import com.runcheck.ui.components.SegmentedBarLegend
import com.runcheck.ui.components.TrendChart
import com.runcheck.ui.learn.LearnTopic
import com.runcheck.ui.learn.RelatedArticlesSection
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.numericRingValueTextStyle
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColorForStoragePercent
import com.runcheck.ui.theme.statusColors

@Composable
fun StorageDetailScreen(
    onBack: () -> Unit,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onNavigateToLearnArticle: (articleId: String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    var mediaPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val requiredMediaPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    val missingMediaPermissions = requiredMediaPermissions.filter { permission ->
        ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
    }
    val shouldOpenMediaSettings = mediaPermissionRequested &&
        missingMediaPermissions.isNotEmpty() &&
        activity?.let { hostActivity ->
            missingMediaPermissions.none { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(hostActivity, permission)
            }
        } == true

    // Trash delete launcher
    val trashDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashEmptied()
        }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.trashDeleteRequestUris.collect { uris ->
            viewModel.onTrashDeleteRequestConsumed()
            when (val requestResult = buildMediaDeleteRequest(context, uris)) {
                is MediaDeleteRequestResult.Ready -> {
                    trashDeleteLauncher.launch(requestResult.request)
                }
                is MediaDeleteRequestResult.Failed -> {
                    viewModel.onTrashDeleteRequestFailed(context.getString(requestResult.messageRes))
                }
            }
        }
    }

    // Request media permissions only after showing an inline rationale card.
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.refresh()
        }
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.startObserving()
                Lifecycle.Event.ON_STOP -> viewModel.stopObserving()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.startObserving()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopObserving()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(
            title = "",
            onBack = onBack
        )
        when (val state = uiState) {
            is StorageUiState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = context.getString(R.string.a11y_loading); liveRegion = LiveRegionMode.Polite },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is StorageUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is StorageUiState.Success -> {
                StorageContent(
                    state = state,
                    hasMediaPermissions = missingMediaPermissions.isEmpty(),
                    shouldOpenMediaSettings = shouldOpenMediaSettings,
                    onRequestMediaPermissions = {
                        if (shouldOpenMediaSettings) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        } else {
                            mediaPermissionRequested = true
                            mediaPermissionLauncher.launch(requiredMediaPermissions.toTypedArray())
                        }
                    },
                    onRefresh = { viewModel.refresh() },
                    onNavigateToCleanup = onNavigateToCleanup,
                    onUpgradeToPro = onUpgradeToPro,
                    onNavigateToLearnArticle = onNavigateToLearnArticle,
                    onEmptyTrash = { viewModel.emptyTrash() },
                    onDismissInfoCard = { viewModel.dismissInfoCard(it) },
                    onPeriodChange = { viewModel.setHistoryPeriod(it) }
                )
            }
        }
    }
}

@Composable
private fun StorageContent(
    state: StorageUiState.Success,
    hasMediaPermissions: Boolean,
    shouldOpenMediaSettings: Boolean,
    onRequestMediaPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onNavigateToLearnArticle: (articleId: String) -> Unit = {},
    onEmptyTrash: () -> Unit = {},
    onDismissInfoCard: (String) -> Unit = {},
    onPeriodChange: (HistoryPeriod) -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
    var activeInfoSheet by rememberSaveable { mutableStateOf<String?>(null) }
    val storage = state.storageState
    val context = LocalContext.current

    LaunchedEffect(state) {
        isRefreshing = false
    }

    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            onRefresh()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            // ── Hero card ──────────────────────────────────────────────
            StorageHeroCard(storage = storage, onInfoClick = { activeInfoSheet = it })

            // ── Info cards ─────────────────────────────────────────────
            if (storage.usagePercent > 75f) {
                InfoCard(
                    id = InfoCardCatalog.StorageFullSlowsPhone.id,
                    headline = stringResource(InfoCardCatalog.StorageFullSlowsPhone.headlineRes),
                    body = stringResource(InfoCardCatalog.StorageFullSlowsPhone.bodyRes),
                    onDismiss = { onDismissInfoCard(it) },
                    visible = InfoCardCatalog.StorageFullSlowsPhone.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.StorageFullSlowsPhone.learnArticleId?.let(onNavigateToLearnArticle)
                    }
                )
            }

            if (!hasMediaPermissions) {
                StorageMediaPermissionCard(
                    shouldOpenSettings = shouldOpenMediaSettings,
                    onAction = onRequestMediaPermissions
                )
            }

            // ── Media Breakdown ────────────────────────────────────────
            storage.mediaBreakdown?.let { breakdown ->
                StorageMediaBreakdownCard(breakdown = breakdown, usedBytes = storage.usedBytes)
            }

            if (hasMediaPermissions && storage.mediaBreakdown != null) {
                InfoCard(
                    id = InfoCardCatalog.StorageOverview.id,
                    headline = stringResource(InfoCardCatalog.StorageOverview.headlineRes),
                    body = stringResource(InfoCardCatalog.StorageOverview.bodyRes),
                    onDismiss = { onDismissInfoCard(it) },
                    visible = InfoCardCatalog.StorageOverview.id !in state.dismissedInfoCards && state.showInfoCards,
                    onLearnMore = {
                        InfoCardCatalog.StorageOverview.learnArticleId?.let(onNavigateToLearnArticle)
                    }
                )
            }

            // ── History chart (Pro only) ─────────────────────────────
            if (state.isPro) {
                StorageHistoryCard(
                    history = state.storageHistory,
                    selectedPeriod = state.selectedHistoryPeriod,
                    historyLoadError = state.historyLoadError,
                    onPeriodChange = onPeriodChange
                )
            }

            // ── Cleanup Tools ──────────────────────────────────────────
            if (state.isPro) {
                StorageCleanupToolsSection(
                    storage = storage,
                    onNavigateToCleanup = onNavigateToCleanup,
                    onEmptyTrash = onEmptyTrash
                )
            } else {
                SectionHeader(text = stringResource(R.string.storage_cleanup_tools))
                ProFeatureCalloutCard(
                    message = stringResource(R.string.pro_feature_cleanup_message),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro
                )
            }

            // ── Details ────────────────────────────────────────────────
            StorageDetailsCard(storage = storage, onInfoClick = { activeInfoSheet = it })

            // ── SD Card ────────────────────────────────────────────────
            if (storage.sdCardAvailable) {
                StorageSdCardCard(storage = storage)
            }

            // ── Quick Actions ──────────────────────────────────────────
            StorageQuickActionsCard()

            RelatedArticlesSection(
                topic = LearnTopic.STORAGE,
                onNavigateToArticle = onNavigateToLearnArticle
            )

            DetailScreenAdBanner()
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }

    activeInfoSheet?.let { key ->
        val content = when (key) {
            "usagePercent" -> StorageInfoContent.usagePercent
            "fillRate" -> StorageInfoContent.fillRate
            "cache" -> StorageInfoContent.cache
            "appsTotal" -> StorageInfoContent.appsTotal
            "filesystem" -> StorageInfoContent.filesystem
            "encryption" -> StorageInfoContent.encryption
            else -> null
        }
        content?.let {
            InfoBottomSheet(content = it, onDismiss = { activeInfoSheet = null })
        }
    }
}

@Composable
private fun StorageMediaPermissionCard(
    shouldOpenSettings: Boolean,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.storage_media_permission_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.storage_media_permission_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onAction) {
                Text(
                    text = stringResource(
                        if (shouldOpenSettings) {
                            R.string.storage_media_permission_open_settings
                        } else {
                            R.string.storage_media_permission_grant
                        }
                    )
                )
            }
        }
    }
}

// ── Hero card ──────────────────────────────────────────────────────────────────

@Composable
private fun StorageHeroCard(storage: StorageState, onInfoClick: (String) -> Unit = {}) {
    val context = LocalContext.current
    val usedFormatted = formatStorageSize(context, storage.usedBytes)
    val totalFormatted = formatStorageSize(context, storage.totalBytes)
    val freeFormatted = formatStorageSize(context, storage.availableBytes)
    val usagePercent = storage.usagePercent.toInt().coerceIn(0, 100)
    val usageSummary = "$usedFormatted / $totalFormatted"
    val freeSummary = stringResource(R.string.storage_free_available, freeFormatted)
    val fillRateLabel = stringResource(R.string.storage_fill_rate)
    val fillRateSummary = storage.fillRateEstimate?.let { "$fillRateLabel: ~$it" }
    val statusText = listOfNotNull(freeSummary, fillRateSummary).joinToString(" · ")

    StoragePanel {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                SectionHeader(text = stringResource(R.string.storage_title))
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            ProgressRing(
                progress = (storage.usagePercent / 100f).coerceIn(0f, 1f),
                modifier = Modifier.size(152.dp),
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                progressColor = statusColorForStoragePercent(usagePercent),
                contentDescription = stringResource(
                    R.string.a11y_progress_percent,
                    stringResource(R.string.storage_title),
                    usagePercent
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$usagePercent%",
                        style = MaterialTheme.numericRingValueTextStyle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.storage_used),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            Text(
                text = usageSummary,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = MaterialTheme.numericFontFamily
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base),
                verticalAlignment = Alignment.Bottom
            ) {
                storage.totalCacheBytes?.let { cache ->
                    MetricPill(
                        label = stringResource(R.string.storage_cache_total),
                        value = formatStorageSize(context, cache),
                        modifier = Modifier.weight(1f),
                        onInfoClick = { onInfoClick("cache") }
                    )
                }
                MetricPill(
                    label = stringResource(R.string.storage_fill_rate),
                    value = storage.fillRateEstimate?.let { "~$it" }
                        ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("fillRate") }
                )
                MetricPill(
                    label = stringResource(R.string.storage_available),
                    value = freeFormatted,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Media Breakdown card ───────────────────────────────────────────────────────

@Composable
private fun StorageMediaBreakdownCard(breakdown: MediaBreakdown, usedBytes: Long) {
    val context = LocalContext.current
    val knownTotal = breakdown.imagesBytes + breakdown.videosBytes + breakdown.audioBytes +
        breakdown.documentsBytes + breakdown.downloadsBytes
    val otherBytes = (usedBytes - knownTotal).coerceAtLeast(0L)

    val segments = listOf(
        SegmentData(
            label = context.getString(R.string.storage_images),
            value = breakdown.imagesBytes,
            formattedValue = formatStorageSize(context, breakdown.imagesBytes),
            color = categoryColor(MediaCategory.IMAGE)
        ),
        SegmentData(
            label = context.getString(R.string.storage_videos),
            value = breakdown.videosBytes,
            formattedValue = formatStorageSize(context, breakdown.videosBytes),
            color = categoryColor(MediaCategory.VIDEO)
        ),
        SegmentData(
            label = context.getString(R.string.storage_audio),
            value = breakdown.audioBytes,
            formattedValue = formatStorageSize(context, breakdown.audioBytes),
            color = categoryColor(MediaCategory.AUDIO)
        ),
        SegmentData(
            label = context.getString(R.string.storage_documents),
            value = breakdown.documentsBytes,
            formattedValue = formatStorageSize(context, breakdown.documentsBytes),
            color = categoryColor(MediaCategory.DOCUMENT)
        ),
        SegmentData(
            label = context.getString(R.string.storage_downloads),
            value = breakdown.downloadsBytes,
            formattedValue = formatStorageSize(context, breakdown.downloadsBytes),
            color = categoryColor(MediaCategory.DOWNLOAD)
        ),
        SegmentData(
            label = context.getString(R.string.storage_other),
            value = otherBytes,
            formattedValue = formatStorageSize(context, otherBytes),
            color = categoryColor(MediaCategory.OTHER)
        )
    )

    StoragePanel {
        CardSectionTitle(text = stringResource(R.string.storage_media_breakdown))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        SegmentedBar(segments = segments)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        SegmentedBarLegend(segments = segments)
    }
}

// ── History chart card ─────────────────────────────────────────────────────────

@Composable
private fun StorageHistoryCard(
    history: List<StorageReading>,
    selectedPeriod: HistoryPeriod,
    historyLoadError: UiText?,
    onPeriodChange: (HistoryPeriod) -> Unit
) {
    var selectedMetric by rememberSaveable { mutableStateOf(StorageHistoryMetric.USED_SPACE.name) }

    val metric = StorageHistoryMetric.entries.firstOrNull { it.name == selectedMetric }
        ?: StorageHistoryMetric.USED_SPACE

    val chartModel = remember(history, metric, selectedPeriod) {
        buildStorageHistoryChartModel(
            history = history,
            metric = metric,
            period = selectedPeriod,
            maxPoints = MAX_STORAGE_HISTORY_POINTS
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
        ) {
            CardSectionTitle(text = stringResource(R.string.storage_history))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                StorageHistoryMetric.entries.forEach { m ->
                    FilterChip(
                        selected = metric == m,
                        onClick = { selectedMetric = m.name },
                        label = { Text(storageHistoryMetricLabel(m)) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
            ) {
                HistoryPeriod.entries
                    .filter { it != HistoryPeriod.SINCE_UNPLUG }
                    .forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { onPeriodChange(period) },
                            label = { Text(historyPeriodLabel(period)) }
                        )
                    }
            }

            historyLoadError?.let { error ->
                Text(
                    text = error.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (chartModel.chartData.size >= 2) {
                Text(
                    text = "${historyPeriodLabel(selectedPeriod)} \u00B7 ${storageHistoryMetricLabel(metric)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                ExpandableChartContainer(
                    onExpand = {}
                ) {
                    TrendChart(
                        data = chartModel.chartData,
                        modifier = Modifier.fillMaxWidth(),
                        yLabels = chartModel.yLabels.ifEmpty { null },
                        xLabels = chartModel.xLabels.ifEmpty { null },
                        showGrid = true,
                        tooltipFormatter = { index -> formatChartTooltip(chartModel, index) }
                    )
                }

                // Min / Avg / Max summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
                ) {
                    chartModel.minValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_min),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    chartModel.averageValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_avg),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    chartModel.maxValue?.let {
                        MetricPill(
                            label = stringResource(R.string.chart_stat_max),
                            value = "${formatDecimal(it, chartModel.tooltipDecimals)}${chartModel.unit}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.network_history_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Cleanup Tools section ──────────────────────────────────────────────────────

@Composable
private fun StorageCleanupToolsSection(
    storage: StorageState,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onEmptyTrash: () -> Unit = {}
) {
    val context = LocalContext.current

    SectionHeader(text = stringResource(R.string.storage_cleanup_tools))

    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        ActionCard(
            icon = Icons.Outlined.FolderOpen,
            iconTint = MaterialTheme.statusColors.poor,
            title = stringResource(R.string.storage_large_files),
            subtitle = stringResource(R.string.storage_large_files_desc),
            actionLabel = stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.LARGE_FILES)
            }
        )

        ActionCard(
            icon = Icons.Outlined.Download,
            iconTint = MaterialTheme.colorScheme.primary,
            title = stringResource(R.string.storage_old_downloads),
            subtitle = stringResource(R.string.storage_old_downloads_desc),
            actionLabel = stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.OLD_DOWNLOADS)
            }
        )

        ActionCard(
            icon = Icons.Outlined.PhoneAndroid,
            iconTint = categoryColor(MediaCategory.APK),
            title = stringResource(R.string.storage_apk_files),
            subtitle = stringResource(R.string.storage_apk_files_desc),
            actionLabel = stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.APK_FILES)
            }
        )

        // Trash — API 30+ only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storage.trashInfo?.let { trash ->
                ActionCard(
                    icon = Icons.Outlined.Delete,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.storage_trash),
                    subtitle = pluralStringResource(R.plurals.storage_trash_summary, trash.itemCount, formatStorageSize(context, trash.totalBytes), trash.itemCount),
                    actionLabel = stringResource(R.string.storage_empty_trash),
                    onAction = onEmptyTrash
                )
            }
        }
    }
}

// ── Details card ───────────────────────────────────────────────────────────────

@Composable
private fun StorageDetailsCard(storage: StorageState, onInfoClick: (String) -> Unit = {}) {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(text = stringResource(R.string.battery_section_details))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        MetricRow(
            label = stringResource(R.string.storage_total),
            value = formatStorageSize(context, storage.totalBytes)
        )
        MetricRow(
            label = stringResource(R.string.storage_used),
            value = stringResource(R.string.storage_used_with_percent, formatStorageSize(context, storage.usedBytes), storage.usagePercent.toInt()),
            onInfoClick = { onInfoClick("usagePercent") }
        )
        MetricRow(
            label = stringResource(R.string.storage_available),
            value = formatStorageSize(context, storage.availableBytes)
        )
        storage.appsBytes?.let { bytes ->
            MetricRow(
                label = stringResource(R.string.storage_apps),
                value = formatStorageSize(context, bytes),
                onInfoClick = { onInfoClick("appsTotal") }
            )
        }
        storage.totalCacheBytes?.let { cache ->
            val cacheText = if (storage.appCount != null) {
                pluralStringResource(
                    R.plurals.storage_cache_summary,
                    storage.appCount,
                    formatStorageSize(context, cache),
                    storage.appCount
                )
            } else {
                formatStorageSize(context, cache)
            }
            MetricRow(
                label = stringResource(R.string.storage_cache_total),
                value = cacheText,
                onInfoClick = { onInfoClick("cache") }
            )
        }

        // Technical details
        val hasTechDetails = storage.fileSystemType != null ||
            storage.encryptionStatus != null ||
            storage.storageVolumes > 0
        if (hasTechDetails) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
            storage.fileSystemType?.let { fs ->
                MetricRow(
                    label = stringResource(R.string.storage_filesystem),
                    value = fs.uppercase(),
                    onInfoClick = { onInfoClick("filesystem") }
                )
            }
            storage.encryptionStatus?.let { enc ->
                MetricRow(
                    label = stringResource(R.string.storage_encryption),
                    value = enc,
                    onInfoClick = { onInfoClick("encryption") }
                )
            }
            if (storage.storageVolumes > 0) {
                MetricRow(
                    label = stringResource(R.string.storage_volumes),
                    value = storage.storageVolumes.toString(),
                    showDivider = false
                )
            }
        }
    }
}

// ── SD Card card ───────────────────────────────────────────────────────────────

@Composable
private fun StorageSdCardCard(storage: StorageState) {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(text = stringResource(R.string.storage_sd_card))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        storage.sdCardTotalBytes?.let { total ->
            MetricRow(
                label = stringResource(R.string.storage_total),
                value = formatStorageSize(context, total)
            )
        }
        storage.sdCardAvailableBytes?.let { available ->
            MetricRow(
                label = stringResource(R.string.storage_available),
                value = formatStorageSize(context, available),
                showDivider = false
            )
        }
    }
}

// ── Quick Actions card ─────────────────────────────────────────────────────────

@Composable
private fun StorageQuickActionsCard() {
    val context = LocalContext.current

    StoragePanel {
        CardSectionTitle(text = stringResource(R.string.storage_quick_actions))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        ListRow(
            label = stringResource(R.string.storage_open_settings),
            icon = Icons.Outlined.Storage,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
            }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
        ListRow(
            label = stringResource(R.string.storage_free_up_space),
            icon = Icons.Outlined.FolderOpen,
            onClick = {
                context.startActivity(Intent("android.os.storage.action.MANAGE_STORAGE"))
            }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
        ListRow(
            label = stringResource(R.string.storage_usage_access),
            icon = Icons.Outlined.Settings,
            onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )
    }
}

// ── Shared panel ───────────────────────────────────────────────────────────────

@Composable
private fun StoragePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
            content = content
        )
    }
}
