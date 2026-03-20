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
import androidx.core.content.ContextCompat
import com.runcheck.ui.ads.DetailScreenAdBanner
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
import com.runcheck.domain.model.MediaBreakdown
import com.runcheck.domain.model.TrashInfo
import com.runcheck.domain.model.StorageState
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.components.ActionCard
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.DetailTopBar
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
import com.runcheck.ui.theme.AccentBlue
import com.runcheck.ui.theme.AccentLime
import com.runcheck.ui.theme.AccentOrange
import com.runcheck.ui.theme.AccentRed
import com.runcheck.ui.theme.AccentTeal
import com.runcheck.ui.theme.AccentYellow
import com.runcheck.ui.theme.TextMuted
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors

@Composable
fun StorageDetailScreen(
    onBack: () -> Unit,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Trash delete launcher
    val trashDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashEmptied()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.trashDeleteIntent.collect { pendingIntent ->
            trashDeleteLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            )
        }
    }

    // Request media permissions for accurate media breakdown on Android 13+
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val needed = listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            ).filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                mediaPermissionLauncher.launch(needed.toTypedArray())
            }
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val needed = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, needed) != PackageManager.PERMISSION_GRANTED) {
                mediaPermissionLauncher.launch(arrayOf(needed))
            }
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
                    onRefresh = { viewModel.refresh() },
                    onNavigateToCleanup = onNavigateToCleanup,
                    onUpgradeToPro = onUpgradeToPro,
                    onEmptyTrash = { viewModel.emptyTrash() }
                )
            }
        }
    }
}

@Composable
private fun StorageContent(
    state: StorageUiState.Success,
    onRefresh: () -> Unit,
    onNavigateToCleanup: (com.runcheck.ui.storage.cleanup.CleanupType) -> Unit = {},
    onUpgradeToPro: () -> Unit = {},
    onEmptyTrash: () -> Unit = {}
) {
    var isRefreshing by remember { mutableStateOf(false) }
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
            StorageHeroCard(storage = storage)

            // ── Media Breakdown ────────────────────────────────────────
            storage.mediaBreakdown?.let { breakdown ->
                StorageMediaBreakdownCard(breakdown = breakdown, usedBytes = storage.usedBytes)
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
            StorageDetailsCard(storage = storage)

            // ── SD Card ────────────────────────────────────────────────
            if (storage.sdCardAvailable) {
                StorageSdCardCard(storage = storage)
            }

            // ── Quick Actions ──────────────────────────────────────────
            StorageQuickActionsCard()

            DetailScreenAdBanner()

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

// ── Hero card ──────────────────────────────────────────────────────────────────

@Composable
private fun StorageHeroCard(storage: StorageState) {
    val context = LocalContext.current
    val usedFormatted = formatStorageSize(context, storage.usedBytes)
    val totalFormatted = formatStorageSize(context, storage.totalBytes)
    val freeFormatted = formatStorageSize(context, storage.availableBytes)
    val progressColor = storageStatusColor(storage.usagePercent)

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
                progressColor = progressColor,
                contentDescription = stringResource(
                    R.string.a11y_progress_percent,
                    stringResource(R.string.storage_title),
                    storage.usagePercent.toInt()
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = usedFormatted,
                        fontSize = 28.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MaterialTheme.numericFontFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = totalFormatted,
                        fontSize = 14.sp,
                        fontFamily = MaterialTheme.numericFontFamily,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))

            val statusParts = buildList {
                add(stringResource(R.string.storage_free_available, freeFormatted))
                storage.fillRateBytesPerDay?.let { r -> add(stringResource(R.string.storage_fill_rate_value, formatStorageSize(context, kotlin.math.abs(r)))) }
            }
            Text(
                text = statusParts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
            ) {
                storage.totalCacheBytes?.let { cache ->
                    MetricPill(
                        label = stringResource(R.string.storage_cache_total),
                        value = formatStorageSize(context, cache),
                        modifier = Modifier.weight(1f)
                    )
                }
                MetricPill(
                    label = stringResource(R.string.storage_fill_rate),
                    value = storage.fillRateEstimate?.let { "~$it" }
                        ?: stringResource(R.string.battery_estimating),
                    modifier = Modifier.weight(1f)
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

    val segments = remember(breakdown, otherBytes, context) {
        listOf(
            SegmentData(
                label = context.getString(R.string.storage_images),
                value = breakdown.imagesBytes,
                formattedValue = formatStorageSize(context, breakdown.imagesBytes),
                color = AccentTeal
            ),
            SegmentData(
                label = context.getString(R.string.storage_videos),
                value = breakdown.videosBytes,
                formattedValue = formatStorageSize(context, breakdown.videosBytes),
                color = AccentBlue
            ),
            SegmentData(
                label = context.getString(R.string.storage_audio),
                value = breakdown.audioBytes,
                formattedValue = formatStorageSize(context, breakdown.audioBytes),
                color = AccentOrange
            ),
            SegmentData(
                label = context.getString(R.string.storage_documents),
                value = breakdown.documentsBytes,
                formattedValue = formatStorageSize(context, breakdown.documentsBytes),
                color = AccentLime
            ),
            SegmentData(
                label = context.getString(R.string.storage_downloads),
                value = breakdown.downloadsBytes,
                formattedValue = formatStorageSize(context, breakdown.downloadsBytes),
                color = AccentYellow
            ),
            SegmentData(
                label = context.getString(R.string.storage_other),
                value = otherBytes,
                formattedValue = formatStorageSize(context, otherBytes),
                color = TextMuted
            )
        )
    }

    StoragePanel {
        CardSectionTitle(text = stringResource(R.string.storage_media_breakdown))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        SegmentedBar(segments = segments)
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        SegmentedBarLegend(segments = segments)
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
            iconTint = AccentOrange,
            title = stringResource(R.string.storage_large_files),
            subtitle = stringResource(R.string.storage_large_files_desc),
            actionLabel = stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.LARGE_FILES)
            }
        )

        ActionCard(
            icon = Icons.Outlined.Download,
            iconTint = AccentBlue,
            title = stringResource(R.string.storage_old_downloads),
            subtitle = stringResource(R.string.storage_old_downloads_desc),
            actionLabel = stringResource(R.string.storage_scan),
            onAction = {
                onNavigateToCleanup(com.runcheck.ui.storage.cleanup.CleanupType.OLD_DOWNLOADS)
            }
        )

        ActionCard(
            icon = Icons.Outlined.PhoneAndroid,
            iconTint = AccentLime,
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
                    iconTint = AccentRed,
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
private fun StorageDetailsCard(storage: StorageState) {
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
            value = stringResource(R.string.storage_used_with_percent, formatStorageSize(context, storage.usedBytes), storage.usagePercent.toInt())
        )
        MetricRow(
            label = stringResource(R.string.storage_available),
            value = formatStorageSize(context, storage.availableBytes)
        )
        storage.appsBytes?.let { bytes ->
            MetricRow(
                label = stringResource(R.string.storage_apps),
                value = formatStorageSize(context, bytes)
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
                value = cacheText
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
                    value = fs.uppercase()
                )
            }
            storage.encryptionStatus?.let { enc ->
                MetricRow(
                    label = stringResource(R.string.storage_encryption),
                    value = enc
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
        shape = RoundedCornerShape(16.dp)
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

// ── Helpers ────────────────────────────────────────────────────────────────────

@Composable
private fun storageStatusColor(usagePercent: Float) = when {
    usagePercent < 70f -> MaterialTheme.statusColors.healthy
    usagePercent < 85f -> MaterialTheme.statusColors.fair
    usagePercent < 95f -> AccentOrange
    else -> MaterialTheme.statusColors.critical
}
