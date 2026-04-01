package com.runcheck.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import com.runcheck.R
import com.runcheck.ui.common.resolve

internal data class SettingsDialogHandles(
    val showResetThresholdsDialog: MutableState<Boolean>,
    val showResetTipsDialog: MutableState<Boolean>,
    val showClearSpeedTestsDialog: MutableState<Boolean>,
    val showNotifPermissionDeniedDialog: MutableState<Boolean>,
    val showClearDialog: MutableState<Boolean>,
)

internal data class SettingsDialogActions(
    val onConfirmResetThresholds: () -> Unit,
    val onConfirmResetTips: () -> Unit,
    val onConfirmClearSpeedTests: () -> Unit,
    val onOpenNotificationSettings: () -> Unit,
    val onConfirmClearDialog: () -> Unit,
)

internal data class SettingsTransientEffectActions(
    val onClearBillingStatus: () -> Unit,
    val onClearExportStatus: () -> Unit,
    val onClearClearDataStatus: () -> Unit,
    val onClearDebugStatus: () -> Unit,
    val onClearExportUris: () -> Unit,
    val onClearErrorMessage: () -> Unit,
)

@Composable
internal fun SettingsTransientEffects(
    uiState: SettingsUiState,
    context: Context,
    actions: SettingsTransientEffectActions,
) {
    val currentActions = rememberUpdatedState(actions)

    uiState.billingStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentActions.value.onClearBillingStatus()
        }
    }
    uiState.exportStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentActions.value.onClearExportStatus()
        }
    }
    uiState.clearDataStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentActions.value.onClearClearDataStatus()
        }
    }
    uiState.debugStatus?.let { status ->
        LaunchedEffect(status) {
            Toast.makeText(context, status.resolve(context), Toast.LENGTH_SHORT).show()
            currentActions.value.onClearDebugStatus()
        }
    }
    uiState.exportUris?.let { exportUriStrings ->
        LaunchedEffect(exportUriStrings) {
            shareExportUris(context, exportUriStrings)
            currentActions.value.onClearExportUris()
        }
    }
    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message.resolve(context), Toast.LENGTH_SHORT).show()
            currentActions.value.onClearErrorMessage()
        }
    }
}

@Composable
internal fun SettingsDialogs(
    handles: SettingsDialogHandles,
    actions: SettingsDialogActions,
) {
    if (handles.showResetThresholdsDialog.value) {
        AlertDialog(
            onDismissRequest = { handles.showResetThresholdsDialog.value = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_thresholds_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_thresholds_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        handles.showResetThresholdsDialog.value = false
                        actions.onConfirmResetThresholds()
                    },
                ) { Text(stringResource(R.string.settings_reset_thresholds)) }
            },
            dismissButton = {
                TextButton(onClick = { handles.showResetThresholdsDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (handles.showResetTipsDialog.value) {
        AlertDialog(
            onDismissRequest = { handles.showResetTipsDialog.value = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_reset_tips_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_tips_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        handles.showResetTipsDialog.value = false
                        actions.onConfirmResetTips()
                    },
                ) { Text(stringResource(R.string.settings_reset_tips)) }
            },
            dismissButton = {
                TextButton(onClick = { handles.showResetTipsDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (handles.showClearSpeedTestsDialog.value) {
        AlertDialog(
            onDismissRequest = { handles.showClearSpeedTestsDialog.value = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_speed_tests_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        handles.showClearSpeedTestsDialog.value = false
                        actions.onConfirmClearSpeedTests()
                    },
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = { handles.showClearSpeedTestsDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (handles.showNotifPermissionDeniedDialog.value) {
        AlertDialog(
            onDismissRequest = { handles.showNotifPermissionDeniedDialog.value = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.notification_permission_denied_title)) },
            text = { Text(stringResource(R.string.notification_permission_denied_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        handles.showNotifPermissionDeniedDialog.value = false
                        actions.onOpenNotificationSettings()
                    },
                ) { Text(stringResource(R.string.notification_permission_denied_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { handles.showNotifPermissionDeniedDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }

    if (handles.showClearDialog.value) {
        AlertDialog(
            onDismissRequest = { handles.showClearDialog.value = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        handles.showClearDialog.value = false
                        actions.onConfirmClearDialog()
                    },
                ) { Text(stringResource(R.string.settings_clear_action)) }
            },
            dismissButton = {
                TextButton(onClick = { handles.showClearDialog.value = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
