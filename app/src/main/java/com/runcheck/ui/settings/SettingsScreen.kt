package com.runcheck.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.runcheck.BuildConfig
import com.runcheck.R
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.service.monitor.NotificationHelper
import com.runcheck.service.monitor.RealTimeMonitorService
import com.runcheck.ui.common.findActivity
import com.runcheck.ui.common.formatStorageSize
import com.runcheck.ui.common.formatTemperature
import com.runcheck.ui.components.CardSectionTitle
import com.runcheck.ui.components.ContentContainer
import com.runcheck.ui.components.DetailTopBar
import com.runcheck.ui.components.MetricPill
import com.runcheck.ui.components.info.InfoSheetContent
import com.runcheck.ui.components.info.InfoSheetHost
import com.runcheck.ui.components.info.rememberInfoSheetState
import com.runcheck.ui.learn.LearnArticleIds
import com.runcheck.ui.theme.numericFontFamily
import com.runcheck.ui.theme.runcheckCardColors
import com.runcheck.ui.theme.runcheckCardElevation
import com.runcheck.ui.theme.spacing
import com.runcheck.ui.theme.statusColors
import com.runcheck.util.ReleaseSafeLog
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val TAG = "SettingsScreen"
internal const val DefaultAlertBatteryThreshold = 20
internal const val DefaultAlertTemperatureThreshold = 42
internal const val DefaultAlertStorageThreshold = 90
internal const val DisabledContentAlpha = 0.38f

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToLearnArticle: (String) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()
    val isXiaomiFamilyDevice =
        remember {
            when (Build.MANUFACTURER.lowercase()) {
                "xiaomi", "redmi", "poco" -> true
                else -> false
            }
        }

    // Notification permission handling — re-checked every time the screen resumes
    // so that revoking permission in system settings is reflected immediately.
    val notificationsPermissionRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    var hasNotificationPermission by remember {
        mutableStateOf(
            !notificationsPermissionRequired ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var alertsEffectivelyEnabled by remember { mutableStateOf(true) }
    var isBatteryOptimizationExempt by remember {
        val pm = context.getSystemService(PowerManager::class.java)
        mutableStateOf(pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true)
    }
    // Track whether the permission request was triggered by the live-notification toggle
    var permissionRequestedForLive by remember { mutableStateOf(false) }
    var showNotifPermissionDeniedDialog by rememberSaveable { mutableStateOf(false) }
    var activeInfoSheet by rememberInfoSheetState()

    LifecycleResumeEffect(Unit) {
        hasNotificationPermission = !notificationsPermissionRequired ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        alertsEffectivelyEnabled = nm.areNotificationsEnabled() &&
            (
                nm.getNotificationChannel(NotificationHelper.CHANNEL_ALERTS)?.importance
                    != android.app.NotificationManager.IMPORTANCE_NONE
            )
        val pm = context.getSystemService(PowerManager::class.java)
        isBatteryOptimizationExempt = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        onPauseOrDispose { }
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { granted ->
            hasNotificationPermission = granted
            if (granted) {
                if (permissionRequestedForLive) {
                    viewModel.setLiveNotificationEnabled(true)
                    val serviceIntent = Intent(context, RealTimeMonitorService::class.java)
                    context.startForegroundService(serviceIntent)
                } else {
                    viewModel.setNotifications(true)
                }
            } else {
                // Detect permanent denial: shouldShowRequestPermissionRationale returns
                // false after the user selected "Don't ask again".
                val permanentlyDenied =
                    activity != null &&
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        )
                if (permanentlyDenied) {
                    showNotifPermissionDeniedDialog = true
                }
            }
            permissionRequestedForLive = false
        }

    val requestNotificationPermission: (Boolean) -> Unit = { forLive ->
        permissionRequestedForLive = forLive
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    val updateLiveNotificationEnabled: (Boolean) -> Unit = { enabled ->
        if (enabled && !hasNotificationPermission && notificationsPermissionRequired) {
            requestNotificationPermission(true)
        } else {
            viewModel.setLiveNotificationEnabled(enabled)
            val serviceIntent = Intent(context, RealTimeMonitorService::class.java)
            if (enabled) {
                context.startForegroundService(serviceIntent)
            } else {
                serviceIntent.action = RealTimeMonitorService.ACTION_STOP
                context.startService(serviceIntent)
            }
        }
    }

    val updateNotificationsEnabled: (Boolean) -> Unit = { enabled ->
        if (enabled && !hasNotificationPermission && notificationsPermissionRequired) {
            requestNotificationPermission(false)
        } else {
            viewModel.setNotifications(enabled)
        }
    }

    // Confirmation dialog states
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showClearSpeedTestsDialog by rememberSaveable { mutableStateOf(false) }
    var showResetTipsDialog by rememberSaveable { mutableStateOf(false) }
    var showResetThresholdsDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        DetailTopBar(title = stringResource(R.string.settings_title), onBack = onBack)

        ContentContainer {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MaterialTheme.spacing.base),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
            ) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
                MonitoringSection(
                    context = context,
                    monitoringInterval = uiState.preferences.monitoringInterval,
                    isBatteryOptimizationExempt = isBatteryOptimizationExempt,
                    onSetMonitoringInterval = viewModel::setMonitoringInterval,
                    onNavigateToMonitoringHelp = {
                        onNavigateToLearnArticle(LearnArticleIds.BACKGROUND_MONITORING)
                    },
                )

                LiveNotificationSection(
                    preferences = uiState.preferences,
                    onSetLiveNotificationEnabled = updateLiveNotificationEnabled,
                    onSetLiveNotifCurrent = viewModel::setLiveNotifCurrent,
                    onSetLiveNotifDrainRate = viewModel::setLiveNotifDrainRate,
                    onSetLiveNotifTemperature = viewModel::setLiveNotifTemperature,
                    onSetLiveNotifScreenStats = viewModel::setLiveNotifScreenStats,
                    onSetLiveNotifRemainingTime = viewModel::setLiveNotifRemainingTime,
                )

                NotificationsSection(
                    context = context,
                    preferences = uiState.preferences,
                    alertsEffectivelyEnabled = alertsEffectivelyEnabled,
                    isXiaomiFamilyDevice = isXiaomiFamilyDevice,
                    onSetNotificationsEnabled = updateNotificationsEnabled,
                    onSetNotifLowBattery = viewModel::setNotifLowBattery,
                    onSetNotifHighTemp = viewModel::setNotifHighTemp,
                    onSetNotifLowStorage = viewModel::setNotifLowStorage,
                    onSetNotifChargeComplete = viewModel::setNotifChargeComplete,
                )

                AlertThresholdsSection(
                    context = context,
                    preferences = uiState.preferences,
                    onSetAlertBatteryThreshold = viewModel::setAlertBatteryThreshold,
                    onSetAlertTempThreshold = viewModel::setAlertTempThreshold,
                    onSetAlertStorageThreshold = viewModel::setAlertStorageThreshold,
                    onResetThresholdsClick = { showResetThresholdsDialog = true },
                )

                DisplaySection(
                    preferences = uiState.preferences,
                    onSetTemperatureUnit = viewModel::setTemperatureUnit,
                    onSetShowInfoCards = viewModel::setShowInfoCards,
                )

                DataSection(
                    uiState = uiState,
                    onSetDataRetention = viewModel::setDataRetention,
                    onExportData = viewModel::exportData,
                    onResetTipsClick = { showResetTipsDialog = true },
                    onClearSpeedTestsClick = { showClearSpeedTestsDialog = true },
                    onClearAllDataClick = { showClearDialog = true },
                )

                ProSection(
                    uiState = uiState,
                    onPurchasePro = { activity?.let(viewModel::purchasePro) },
                    onRefreshPurchaseStatus = viewModel::refreshPurchaseStatus,
                )

                SettingsMeasurementSection(
                    uiState = uiState,
                    context = context,
                    onInfoClick = { activeInfoSheet = it },
                )

                SettingsAboutSection(context = context)

                SettingsTransientEffects(
                    uiState = uiState,
                    context = context,
                    onClearBillingStatus = { viewModel.clearBillingStatus() },
                    onClearExportStatus = { viewModel.clearExportStatus() },
                    onClearClearDataStatus = { viewModel.clearClearDataStatus() },
                    onClearExportUris = { viewModel.clearExportUris() },
                    onClearErrorMessage = { viewModel.clearErrorMessage() },
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.xl))
            }
        }
    }

    InfoSheetHost(
        activeKey = activeInfoSheet,
        onDismiss = { activeInfoSheet = null },
        resolveContent = ::resolveSettingsInfoContent,
    )
    val resetTipsDoneMessage = stringResource(R.string.settings_reset_tips_done)

    SettingsDialogs(
        showResetThresholdsDialog = showResetThresholdsDialog,
        onDismissResetThresholds = { showResetThresholdsDialog = false },
        onConfirmResetThresholds = { viewModel.resetAlertThresholds() },
        showResetTipsDialog = showResetTipsDialog,
        onDismissResetTips = { showResetTipsDialog = false },
        onConfirmResetTips = {
            viewModel.resetTips()
            Toast
                .makeText(
                    context,
                    resetTipsDoneMessage,
                    Toast.LENGTH_SHORT,
                ).show()
        },
        showClearSpeedTestsDialog = showClearSpeedTestsDialog,
        onDismissClearSpeedTests = { showClearSpeedTestsDialog = false },
        onConfirmClearSpeedTests = { viewModel.clearSpeedTests() },
        showNotifPermissionDeniedDialog = showNotifPermissionDeniedDialog,
        onDismissNotifPermissionDenied = { showNotifPermissionDeniedDialog = false },
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                },
            )
        },
        showClearDialog = showClearDialog,
        onDismissClearDialog = { showClearDialog = false },
        onConfirmClearDialog = { viewModel.clearAllData() },
    )
}

@Composable
private fun SettingsMeasurementSection(
    uiState: SettingsUiState,
    context: android.content.Context,
    onInfoClick: (String) -> Unit,
) {
    uiState.deviceProfile?.let { profile ->
        SettingsCard {
            CardSectionTitle(text = stringResource(R.string.settings_measurement_info))
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text = stringResource(R.string.settings_device_model, profile.manufacturer, profile.model),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
            Text(
                text =
                    stringResource(
                        if (profile.currentNowReliable) {
                            R.string.settings_current_support_summary_reliable
                        } else {
                            R.string.settings_current_support_summary_unreliable
                        },
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.settings_api_level_label),
                    value = androidVersionName(profile.apiLevel),
                    modifier = Modifier.weight(1f),
                )
                MetricPill(
                    label = stringResource(R.string.settings_current_reading_label),
                    value =
                        stringResource(
                            if (profile.currentNowReliable) {
                                R.string.settings_measurement_reliable
                            } else {
                                R.string.settings_measurement_unreliable
                            },
                        ),
                    valueColor =
                        if (profile.currentNowReliable) {
                            MaterialTheme.statusColors.healthy
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("currentReading") },
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                MetricPill(
                    label = stringResource(R.string.settings_cycle_count_label),
                    value =
                        stringResource(
                            if (profile.cycleCountAvailable) {
                                R.string.settings_measurement_available
                            } else {
                                R.string.settings_measurement_not_available
                            },
                        ),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("cycleCount") },
                )
                MetricPill(
                    label = stringResource(R.string.settings_thermal_zones_label),
                    value = profile.thermalZonesAvailable.size.toString(),
                    modifier = Modifier.weight(1f),
                    onInfoClick = { onInfoClick("thermalZones") },
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.md),
                verticalAlignment = Alignment.Top,
            ) {
                val memoryInfo =
                    remember {
                        val activityManager =
                            context.getSystemService(
                                android.app.ActivityManager::class.java,
                            )
                        android.app.ActivityManager
                            .MemoryInfo()
                            .also { activityManager?.getMemoryInfo(it) }
                    }
                MetricPill(
                    label = stringResource(R.string.settings_ram_label),
                    value = formatStorageSize(context, memoryInfo.totalMem),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingsAboutSection(context: android.content.Context) {
    SettingsCard {
        CardSectionTitle(text = stringResource(R.string.settings_about))
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.xs))
        Text(
            text =
                stringResource(
                    R.string.settings_version,
                    "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_rate),
            onClick = {
                try {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${context.packageName}"),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                } catch (_: Exception) {
                    openExternalUri(
                        context = context,
                        uri = context.getString(R.string.settings_play_store_web_url),
                    )
                }
            },
        )
        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_privacy_policy),
            onClick = {
                openExternalUri(
                    context = context,
                    uri = context.getString(R.string.settings_privacy_policy_url),
                )
            },
        )
        SettingsDivider()
        SettingsNavigationRow(
            label = stringResource(R.string.settings_feedback),
            onClick = {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                context.getString(R.string.feedback_email_subject),
                            )
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                    )
                } catch (e: android.content.ActivityNotFoundException) {
                    ReleaseSafeLog.warn(TAG, "Failed to open email client for feedback", e)
                }
            },
        )
    }
}

@Composable
private fun SettingsTransientEffects(
    uiState: SettingsUiState,
    context: android.content.Context,
    onClearBillingStatus: () -> Unit,
    onClearExportStatus: () -> Unit,
    onClearClearDataStatus: () -> Unit,
    onClearExportUris: () -> Unit,
    onClearErrorMessage: () -> Unit,
) {
    val currentOnClearBillingStatus = rememberUpdatedState(onClearBillingStatus)
    val currentOnClearExportStatus = rememberUpdatedState(onClearExportStatus)
    val currentOnClearClearDataStatus = rememberUpdatedState(onClearClearDataStatus)
    val currentOnClearExportUris = rememberUpdatedState(onClearExportUris)
    val currentOnClearErrorMessage = rememberUpdatedState(onClearErrorMessage)

    uiState.billingStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentOnClearBillingStatus.value()
        }
    }
    uiState.exportStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentOnClearExportStatus.value()
        }
    }
    uiState.clearDataStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentOnClearClearDataStatus.value()
        }
    }
    uiState.exportUris?.let { exportUriStrings ->
        LaunchedEffect(exportUriStrings) {
            shareExportUris(context, exportUriStrings)
            currentOnClearExportUris.value()
        }
    }
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
            currentOnClearErrorMessage.value()
        }
    }
}

@Composable
private fun SettingsDialogs(
    showResetThresholdsDialog: Boolean,
    onDismissResetThresholds: () -> Unit,
    onConfirmResetThresholds: () -> Unit,
    showResetTipsDialog: Boolean,
    onDismissResetTips: () -> Unit,
    onConfirmResetTips: () -> Unit,
    showClearSpeedTestsDialog: Boolean,
    onDismissClearSpeedTests: () -> Unit,
    onConfirmClearSpeedTests: () -> Unit,
    showNotifPermissionDeniedDialog: Boolean,
    onDismissNotifPermissionDenied: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    showClearDialog: Boolean,
    onDismissClearDialog: () -> Unit,
    onConfirmClearDialog: () -> Unit,
) {
    if (showResetThresholdsDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetThresholds,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_thresholds_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_thresholds_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissResetThresholds()
                        onConfirmResetThresholds()
                    },
                ) { Text(stringResource(R.string.settings_reset_thresholds)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetThresholds) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showResetTipsDialog) {
        AlertDialog(
            onDismissRequest = onDismissResetTips,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_tips_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_tips_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissResetTips()
                        onConfirmResetTips()
                    },
                ) { Text(stringResource(R.string.settings_reset_tips)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissResetTips) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showClearSpeedTestsDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearSpeedTests,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissClearSpeedTests()
                        onConfirmClearSpeedTests()
                    },
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearSpeedTests) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showNotifPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = onDismissNotifPermissionDenied,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.notification_permission_denied_title)) },
            text = { Text(stringResource(R.string.notification_permission_denied_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissNotifPermissionDenied()
                        onOpenNotificationSettings()
                    },
                ) { Text(stringResource(R.string.notification_permission_denied_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissNotifPermissionDenied) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = onDismissClearDialog,
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissClearDialog()
                        onConfirmClearDialog()
                    },
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearDialog) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

// ── Reusable settings components ──────────────────────────────────────────────

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = runcheckCardColors(),
        elevation = runcheckCardElevation(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.base),
            content = content,
        )
    }
}

@Composable
internal fun SettingsDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
}

@Composable
internal fun SettingsRadioRow(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .alpha(if (enabled) 1f else DisabledContentAlpha)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    onClick = onSelect,
                    role = Role.RadioButton,
                ).padding(vertical = MaterialTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = MaterialTheme.spacing.sm),
        )
    }
}

@Composable
internal fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .alpha(if (enabled) 1f else DisabledContentAlpha)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    onValueChange = onCheckedChange,
                    role = Role.Switch,
                ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun SettingsSlider(
    label: String,
    value: Int,
    allowedValues: List<Int>,
    valueLabelFor: (Int) -> String,
    onValueChange: (Int) -> Unit,
) {
    var sliderValue by remember(value, allowedValues) {
        mutableFloatStateOf(allowedValues.indexForValue(value).toFloat())
    }
    val currentIndex = sliderValue.roundToInt().coerceIn(0, allowedValues.lastIndex)
    val currentValue = allowedValues[currentIndex]
    val valueLabel = valueLabelFor(currentValue)
    val sliderDescription = stringResource(R.string.value_label_colon, label, valueLabel)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = MaterialTheme.numericFontFamily),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it.coerceIn(0f, allowedValues.lastIndex.toFloat()) },
            onValueChangeFinished = {
                onValueChange(
                    allowedValues[sliderValue.roundToInt().coerceIn(0, allowedValues.lastIndex)],
                )
            },
            valueRange = 0f..allowedValues.lastIndex.toFloat(),
            steps = (allowedValues.size - 2).coerceAtLeast(0),
            modifier =
                Modifier.semantics {
                    contentDescription = sliderDescription
                },
            colors =
                SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
        )
    }
}

@Composable
internal fun SettingsValueRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(vertical = MaterialTheme.spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

@Composable
internal fun SettingsNavigationRow(
    label: String,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(onClick = onClick, role = Role.Button)
                .semantics(mergeDescendants = true) {}
                .padding(vertical = MaterialTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor)
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun DataRetention.label(): String =
    when (this) {
        DataRetention.THREE_MONTHS -> stringResource(R.string.settings_retention_3_months)
        DataRetention.SIX_MONTHS -> stringResource(R.string.settings_retention_6_months)
        DataRetention.ONE_YEAR -> stringResource(R.string.settings_retention_1_year)
        DataRetention.FOREVER -> stringResource(R.string.settings_retention_forever)
    }

private fun List<Int>.indexForValue(value: Int): Int =
    indexOf(value).takeIf { it >= 0 } ?: indices.minBy { index -> abs(this[index] - value) }

private fun openExternalUri(
    context: android.content.Context,
    uri: String,
) {
    context.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun androidVersionName(apiLevel: Int): String =
    when (apiLevel) {
        26 -> "Android 8.0"
        27 -> "Android 8.1"
        28 -> "Android 9"
        29 -> "Android 10"
        30 -> "Android 11"
        31 -> "Android 12"
        32 -> "Android 12L"
        33 -> "Android 13"
        34 -> "Android 14"
        35 -> "Android 15"
        36 -> "Android 16"
        37 -> "Android 17"
        else -> "API $apiLevel"
    }

private fun resolveSettingsInfoContent(key: String): InfoSheetContent? =
    when (key) {
        "currentReading" -> SettingsInfoContent.currentReading
        "cycleCount" -> SettingsInfoContent.cycleCount
        "thermalZones" -> SettingsInfoContent.thermalZones
        else -> null
    }

internal fun shareExportUris(
    context: android.content.Context,
    exportUriStrings: List<String>,
) {
    val parsedUris = exportUriStrings.map(Uri::parse)
    val shareIntent =
        if (parsedUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, parsedUris.first())
                clipData = android.content.ClipData.newRawUri(null, parsedUris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "text/csv"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(parsedUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    val chooserTitle = context.getString(R.string.settings_export_share_title)
    context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
}

internal val LOW_BATTERY_THRESHOLD_VALUES = (5..50 step 5).toList()
internal val TEMPERATURE_THRESHOLD_VALUES = (35..50).toList()
internal val LOW_STORAGE_THRESHOLD_VALUES = listOf(70, 75, 80, 85, 90, 95, 99)
