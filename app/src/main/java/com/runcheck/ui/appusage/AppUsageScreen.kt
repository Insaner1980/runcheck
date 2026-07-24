package com.runcheck.ui.appusage

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.runcheck.R
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.domain.model.UnusedAppCandidate
import com.runcheck.domain.model.UnusedAppsPeriod
import com.runcheck.ui.common.LifecycleStartStopEffect
import com.runcheck.ui.common.resolve
import com.runcheck.ui.components.AppDisplayName
import com.runcheck.ui.components.ExpressiveDetailScaffold
import com.runcheck.ui.components.ExpressiveEmptyState
import com.runcheck.ui.components.ExpressiveSingleChoiceSelector
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.components.RuncheckLoadingIndicator
import com.runcheck.ui.components.resolveAppDisplayName
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
fun AppUsageScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppUsageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unusedAppsState by viewModel.unusedAppsState.collectAsStateWithLifecycle()

    LifecycleStartStopEffect(
        onStart = viewModel::startObserving,
        onStop = viewModel::stopObserving,
    )

    ExpressiveDetailScaffold(
        title = stringResource(R.string.app_usage_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        when (val state = uiState) {
            is AppUsageUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RuncheckLoadingIndicator(
                        contentDescription = stringResource(R.string.a11y_loading),
                    )
                }
            }

            is AppUsageUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message.resolve())
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is AppUsageUiState.Success -> {
                val appItems = viewModel.pagedApps.collectAsLazyPagingItems()
                AppUsageContent(
                    state = state,
                    appItems = appItems,
                    onRefresh = { viewModel.refresh() },
                    unusedAppsState = unusedAppsState,
                    onLoadUnusedApps = viewModel::loadUnusedApps,
                )
            }

            AppUsageUiState.Locked -> {
                val currentOnUpgradeToPro by rememberUpdatedState(onUpgradeToPro)
                LaunchedEffect(Unit) {
                    currentOnUpgradeToPro()
                }
                ProFeatureLockedState(
                    title = stringResource(R.string.app_usage_title),
                    message =
                        stringResource(
                            R.string.pro_feature_locked_message,
                            stringResource(R.string.app_usage_title),
                        ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro,
                )
            }
        }
    }
}

private enum class AppUsageMode {
    USAGE,
    NOT_USED,
}

@Composable
private fun AppUsageContent(
    state: AppUsageUiState.Success,
    appItems: LazyPagingItems<com.runcheck.domain.model.AppBatteryUsage>,
    onRefresh: () -> Unit,
    unusedAppsState: UnusedAppsUiState,
    onLoadUnusedApps: (UnusedAppsPeriod, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val currentOnRefresh by rememberUpdatedState(onRefresh)
    var hasUsageAccess by remember(context) { mutableStateOf(context.hasUsageStatsAccess()) }
    var selectedMode by rememberSaveable { mutableStateOf(AppUsageMode.USAGE) }
    var unusedPeriod by rememberSaveable { mutableStateOf(UnusedAppsPeriod.DAYS_30) }
    var uninstallPending by rememberSaveable { mutableStateOf(false) }
    val maxTime = state.maxForegroundTimeMs.coerceAtLeast(1L)
    val totalTime = state.totalForegroundTimeMs.coerceAtLeast(1L)

    LifecycleResumeEffect(context) {
        val currentAccess = context.hasUsageStatsAccess()
        val justGrantedAccess = !hasUsageAccess && currentAccess
        hasUsageAccess = currentAccess
        if (currentAccess && (justGrantedAccess || appItems.itemCount == 0)) {
            currentOnRefresh()
        }
        if (selectedMode == AppUsageMode.NOT_USED) {
            onLoadUnusedApps(unusedPeriod, true)
            uninstallPending = false
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(selectedMode, unusedPeriod) {
        if (selectedMode == AppUsageMode.NOT_USED) {
            onLoadUnusedApps(unusedPeriod, false)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
    ) {
        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            ExpressiveSingleChoiceSelector(
                options = AppUsageMode.entries,
                selected = selectedMode,
                labelFor = { mode ->
                    stringResource(
                        if (mode == AppUsageMode.USAGE) {
                            R.string.app_usage_mode_usage
                        } else {
                            R.string.app_usage_mode_not_used
                        },
                    )
                },
                onSelect = { selectedMode = it },
            )
        }

        when {
            selectedMode == AppUsageMode.NOT_USED -> {
                item {
                    ExpressiveSingleChoiceSelector(
                options = UnusedAppsPeriod.entries,
                selected = unusedPeriod,
                labelFor = { period ->
                    stringResource(R.string.app_usage_unused_period_days, period.days)
                },
                onSelect = { unusedPeriod = it },
            )
                }
                unusedAppsItems(
                    state = unusedAppsState,
                    context = context,
                    onRetry = { onLoadUnusedApps(unusedPeriod, true) },
                    onUninstall = { packageName ->
                        uninstallPending = true
                        context.startActivity(
                            Intent(Intent.ACTION_DELETE, Uri.parse("package:$packageName")),
                        )
                    },
                )
            }

            !hasUsageAccess -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = runcheckCardColors(),
                        elevation = runcheckCardElevation(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.base),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        ) {
                            Text(
                                text = stringResource(R.string.app_usage_permission_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.app_usage_permission_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK,
                                            ),
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        context.startActivity(
                                            Intent(Settings.ACTION_SETTINGS).addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK,
                                            ),
                                        )
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.app_usage_permission_open_settings))
                            }
                        }
                    }
                }
            }

            appItems.loadState.refresh is LoadState.Loading && appItems.itemCount == 0 -> {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = MaterialTheme.spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        RuncheckLoadingIndicator(
                            contentDescription = stringResource(R.string.a11y_loading),
                        )
                    }
                }
            }

            appItems.loadState.refresh is LoadState.Error && appItems.itemCount == 0 -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = runcheckCardColors(),
                        elevation = runcheckCardElevation(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.base),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        ) {
                            Text(
                                text = stringResource(R.string.common_error_generic),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            TextButton(onClick = { appItems.retry() }) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }
            }

            appItems.itemCount == 0 -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = runcheckCardColors(),
                        elevation = runcheckCardElevation(),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(MaterialTheme.spacing.base),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                        ) {
                            Text(
                                text = stringResource(R.string.app_usage_no_data),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.app_usage_no_data_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = onRefresh) {
                                Text(stringResource(R.string.common_retry))
                            }
                        }
                    }
                }
            }

            else -> {
                items(
                    count = appItems.itemCount,
                    key = appItems.itemKey { it.packageName },
                    contentType = appItems.itemContentType { "app_usage" },
                ) { index ->
                    appItems[index]?.let { app ->
                        AppUsageItem(
                            app = app,
                            maxTime = maxTime,
                            totalTime = totalTime,
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

private fun LazyListScope.unusedAppsItems(
    state: UnusedAppsUiState,
    context: Context,
    onRetry: () -> Unit,
    onUninstall: (String) -> Unit,
) {
    when (state) {
        UnusedAppsUiState.Idle,
        UnusedAppsUiState.Loading,
        -> {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = MaterialTheme.spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    RuncheckLoadingIndicator(
                        contentDescription = context.getString(R.string.a11y_loading),
                    )
                }
            }
        }

        UnusedAppsUiState.Locked -> {
            item {
                ExpressiveEmptyState(
                    title = context.getString(R.string.app_usage_mode_not_used),
                    message = context.getString(R.string.app_usage_unused_requires_pro),
                )
            }
        }

        is UnusedAppsUiState.PermissionRequired -> {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = runcheckCardColors(),
                    elevation = runcheckCardElevation(),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(MaterialTheme.spacing.base),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
                    ) {
                        Text(
                            text = context.getString(R.string.app_usage_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = context.getString(R.string.app_usage_permission_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK,
                                    ),
                                )
                            },
                        ) {
                            Text(context.getString(R.string.app_usage_permission_open_settings))
                        }
                    }
                }
            }
        }

        is UnusedAppsUiState.Error -> {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ExpressiveEmptyState(
                        title = context.getString(R.string.common_error_generic),
                        message = state.message.resolve(),
                    )
                    TextButton(onClick = onRetry) {
                        Text(context.getString(R.string.common_retry))
                    }
                }
            }
        }

        is UnusedAppsUiState.Success -> {
            if (state.partialErrors.isNotEmpty()) {
                item {
                    val messageRes =
                        when (classifyUnusedAppsPartialErrors(state.partialErrors)) {
                            UnusedAppsPartialErrorKind.STORAGE_ONLY -> R.string.app_usage_unused_partial_sizes
                            UnusedAppsPartialErrorKind.LABELS_ONLY -> R.string.app_usage_unused_partial_labels
                            UnusedAppsPartialErrorKind.STORAGE_AND_LABELS ->
                                R.string.app_usage_unused_partial_details

                            UnusedAppsPartialErrorKind.NONE -> return@item
                        }
                    Text(
                        text = context.getString(messageRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.candidates.isEmpty()) {
                item {
                    ExpressiveEmptyState(
                        title = context.getString(R.string.app_usage_unused_none_title),
                        message =
                            context.getString(
                                R.string.app_usage_unused_none_message,
                                state.period.days,
                            ),
                    )
                }
            } else {
                items(
                    items = state.candidates,
                    key = UnusedAppCandidate::packageName,
                ) { candidate ->
                    UnusedAppItem(
                        candidate = candidate,
                        period = state.period,
                        onUninstall = { onUninstall(candidate.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnusedAppItem(
    candidate: UnusedAppCandidate,
    period: UnusedAppsPeriod,
    onUninstall: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(candidate.packageName)
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    AppDisplayName(
                        appLabel = candidate.appLabel.orEmpty(),
                        packageName = candidate.packageName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = candidate.packageName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text =
                    candidate.lastRecordedUse?.let { lastUse ->
                        stringResource(
                            R.string.app_usage_unused_last_recorded,
                            DateFormat.getDateInstance().format(Date.from(lastUse)),
                        )
                    } ?: stringResource(
                        R.string.app_usage_unused_no_recorded,
                        period.days,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    candidate.storageBytes?.let { bytes ->
                        stringResource(R.string.app_usage_unused_size_mb, bytes / (1024 * 1024))
                    } ?: stringResource(R.string.app_usage_unused_size_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onUninstall) {
                Text(stringResource(R.string.app_usage_unused_uninstall))
            }
        }
    }
}

@Composable
private fun AppUsageItem(
    app: AppBatteryUsage,
    maxTime: Long,
    totalTime: Long,
) {
    val hours = app.foregroundTimeMs / 3_600_000
    val minutes = (app.foregroundTimeMs % 3_600_000) / 60_000
    val progress = (app.foregroundTimeMs.toFloat() / maxTime).coerceIn(0f, 1f)
    val percentOfTotal =
        ((app.foregroundTimeMs.toFloat() / totalTime.toFloat()) * 100f)
            .coerceIn(0f, 100f)
    val displayName =
        resolveAppDisplayName(
            appLabel = app.appLabel,
            packageName = app.packageName,
            unknownAppLabel = stringResource(R.string.app_unknown_name),
        )
    val progressDescription =
        stringResource(
            R.string.a11y_progress_percent,
            displayName,
            (progress * 100).roundToInt(),
        )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIcon(packageName = app.packageName)
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AppDisplayName(
                            appLabel = app.appLabel,
                            packageName = app.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            text =
                                if (hours > 0) {
                                    stringResource(R.string.app_usage_time_hours_minutes, hours, minutes)
                                } else {
                                    stringResource(R.string.app_usage_time_minutes, minutes)
                                },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    text =
                        stringResource(
                            R.string.app_usage_percent,
                            percentOfTotal.roundToInt(),
                        ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .semantics {
                            contentDescription = progressDescription
                            progressBarRangeInfo =
                                androidx.compose.ui.semantics
                                    .ProgressBarRangeInfo(progress, 0f..1f)
                        },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

private val appIconCache =
    object : LruCache<String, Bitmap>(MAX_APP_ICON_CACHE_KB) {
        override fun sizeOf(
            key: String,
            value: Bitmap,
        ): Int = value.byteCount / 1024
    }

private const val MAX_APP_ICON_CACHE_KB = 8 * 1024

@Composable
private fun AppIcon(
    packageName: String,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val context = LocalContext.current
    val bitmapState =
        produceState<Bitmap?>(
            initialValue = appIconCache[packageName],
            key1 = packageName,
        ) {
            if (value != null) return@produceState
            value =
                withContext(ioDispatcher) {
                    loadAppIconBitmap(context, packageName)?.also { bitmap ->
                        appIconCache.put(packageName, bitmap)
                    }
                }
        }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
        )
    } else {
        IconCircle(
            icon = Icons.Outlined.Android,
            size = 40.dp,
            iconSize = 20.dp,
            tint = MaterialTheme.statusColors.healthy,
        )
    }
}

private fun loadAppIconBitmap(
    context: Context,
    packageName: String,
): Bitmap? =
    try {
        val appInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(
                        PackageManager.MATCH_UNINSTALLED_PACKAGES.toLong(),
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.MATCH_UNINSTALLED_PACKAGES,
                )
            }
        context.packageManager.getApplicationIcon(appInfo).toBitmap(96, 96)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: RuntimeException) {
        null
    }

private fun Context.hasUsageStatsAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode =
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            packageName,
        )
    return mode == AppOpsManager.MODE_ALLOWED
}
