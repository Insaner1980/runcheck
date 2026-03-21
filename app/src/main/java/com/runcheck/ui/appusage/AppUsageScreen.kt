package com.runcheck.ui.appusage

import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Process
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.runcheck.R
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.ui.components.IconCircle
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun AppUsageScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppUsageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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
            title = stringResource(R.string.app_usage_title),
            onBack = onBack
        )
        when (val state = uiState) {
            is AppUsageUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppUsageUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.common_error_generic))
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
            is AppUsageUiState.Success -> {
                AppUsageContent(
                    state = state,
                    viewModel = viewModel,
                    onRefresh = { viewModel.refresh() }
                )
            }
            AppUsageUiState.Locked -> {
                LaunchedEffect(Unit) {
                    onUpgradeToPro()
                }
                ProFeatureLockedState(
                    title = stringResource(R.string.app_usage_title),
                    message = stringResource(
                        R.string.pro_feature_locked_message,
                        stringResource(R.string.app_usage_title)
                    ),
                    actionLabel = stringResource(R.string.pro_feature_upgrade_action),
                    onAction = onUpgradeToPro
                )
            }
        }
    }
}

@Composable
private fun AppUsageContent(
    state: AppUsageUiState.Success,
    viewModel: AppUsageViewModel,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val appItems = viewModel.pagedApps.collectAsLazyPagingItems()
    var hasUsageAccess by remember(context) { mutableStateOf(context.hasUsageStatsAccess()) }
    val maxTime = state.maxForegroundTimeMs.coerceAtLeast(1L)
    val totalTime = state.totalForegroundTimeMs.coerceAtLeast(1L)

    LifecycleResumeEffect(context) {
        val currentAccess = context.hasUsageStatsAccess()
        val justGrantedAccess = !hasUsageAccess && currentAccess
        hasUsageAccess = currentAccess
        if (currentAccess && (justGrantedAccess || appItems.itemCount == 0)) {
            onRefresh()
        }
        onPauseOrDispose { }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = MaterialTheme.spacing.base),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)
    ) {
        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
        }

        if (!hasUsageAccess) {
            item {
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
                            text = stringResource(R.string.app_usage_permission_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_usage_permission_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_SETTINGS).addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    )
                                }
                            }
                        ) {
                            Text(stringResource(R.string.app_usage_permission_open_settings))
                        }
                    }
                }
            }
        } else if (appItems.loadState.refresh is LoadState.Loading && appItems.itemCount == 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = MaterialTheme.spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else if (appItems.loadState.refresh is LoadState.Error && appItems.itemCount == 0) {
            item {
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
                            text = stringResource(R.string.common_error_generic),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { appItems.retry() }) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
        } else if (appItems.itemCount == 0) {
            item {
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
                            text = stringResource(R.string.app_usage_no_data),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.app_usage_no_data_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onRefresh) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }
        } else {
            items(
                count = appItems.itemCount,
                key = appItems.itemKey { it.packageName },
                contentType = appItems.itemContentType { "app_usage" }
            ) { index ->
                appItems[index]?.let { app ->
                    AppUsageItem(
                        app = app,
                        maxTime = maxTime,
                        totalTime = totalTime
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun AppUsageItem(app: AppBatteryUsage, maxTime: Long, totalTime: Long) {
    val hours = app.foregroundTimeMs / 3_600_000
    val minutes = (app.foregroundTimeMs % 3_600_000) / 60_000
    val progress = (app.foregroundTimeMs.toFloat() / maxTime).coerceIn(0f, 1f)
    val percentOfTotal = ((app.foregroundTimeMs.toFloat() / totalTime.toFloat()) * 100f)
        .coerceIn(0f, 100f)
    val progressDescription = stringResource(
        R.string.a11y_progress_percent,
        app.appLabel,
        (progress * 100).roundToInt()
    )

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
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIcon(
                        packageName = app.packageName,
                        appLabel = app.appLabel
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.sm))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = app.appLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (hours > 0) {
                                stringResource(R.string.app_usage_time_hours_minutes, hours, minutes)
                            } else {
                                stringResource(R.string.app_usage_time_minutes, minutes)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.app_usage_percent,
                        percentOfTotal.roundToInt()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .semantics {
                        contentDescription = progressDescription
                        progressBarRangeInfo =
                            androidx.compose.ui.semantics.ProgressBarRangeInfo(progress, 0f..1f)
                    },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            app.estimatedDrainMah?.let { drain ->
                Text(
                    text = stringResource(R.string.app_usage_drain, drain.toDouble()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val appIconCache = object : LruCache<String, Bitmap>(MAX_APP_ICON_CACHE_KB) {
    override fun sizeOf(key: String, value: Bitmap): Int =
        value.byteCount / 1024
}

private const val MAX_APP_ICON_CACHE_KB = 8 * 1024

@Composable
private fun AppIcon(packageName: String, appLabel: String) {
    val context = LocalContext.current
    val bitmapState = produceState<Bitmap?>(
        initialValue = appIconCache.get(packageName),
        key1 = packageName
    ) {
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) {
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
            modifier = Modifier.size(40.dp)
        )
    } else {
        IconCircle(
            icon = Icons.Outlined.Android,
            size = 40.dp,
            iconSize = 20.dp,
            tint = MaterialTheme.statusColors.healthy
        )
    }
}

private fun loadAppIconBitmap(context: Context, packageName: String): Bitmap? {
    return try {
        context.packageManager.getApplicationIcon(packageName).toBitmap(96, 96)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: RuntimeException) {
        null
    }
}

private fun Context.hasUsageStatsAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
