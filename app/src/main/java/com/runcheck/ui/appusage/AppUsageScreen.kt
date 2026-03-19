package com.runcheck.ui.appusage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.runcheck.R
import com.runcheck.domain.model.AppBatteryUsage
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.ProFeatureLockedState
import com.runcheck.ui.theme.spacing
import kotlin.math.roundToInt

@Composable
fun AppUsageScreen(
    onBack: () -> Unit,
    onUpgradeToPro: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppUsageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                AppUsageContent(state = state)
            }
            AppUsageUiState.Locked -> {
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
private fun AppUsageContent(state: AppUsageUiState.Success) {
    val context = LocalContext.current
    var hasUsageAccess by remember(context) { mutableStateOf(context.hasUsageStatsAccess()) }
    val maxTime = remember(state.apps) {
        state.apps.maxOfOrNull { it.foregroundTimeMs } ?: 1L
    }

    LifecycleResumeEffect(context) {
        hasUsageAccess = context.hasUsageStatsAccess()
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
                    shape = RoundedCornerShape(16.dp),
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
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                            }
                        ) {
                            Text(stringResource(R.string.app_usage_permission_open_settings))
                        }
                    }
                }
            }
        } else if (state.apps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.app_usage_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = state.apps,
                key = { it.packageName },
                contentType = { "app_usage" }
            ) { app ->
                AppUsageItem(app = app, maxTime = maxTime)
            }
        }

        item {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.md))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun AppUsageItem(app: AppBatteryUsage, maxTime: Long) {
    val context = LocalContext.current
    val hours = app.foregroundTimeMs / 3_600_000
    val minutes = (app.foregroundTimeMs % 3_600_000) / 60_000
    val progress = (app.foregroundTimeMs.toFloat() / maxTime).coerceIn(0f, 1f)
    val progressDescription = stringResource(
        R.string.a11y_progress_percent,
        app.appLabel,
        (progress * 100).roundToInt()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                Text(
                    text = app.appLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (hours > 0) {
                        stringResource(R.string.app_usage_time_hours_minutes, hours, minutes)
                    } else {
                        stringResource(R.string.app_usage_time_minutes, minutes)
                    },
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

private fun Context.hasUsageStatsAccess(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
