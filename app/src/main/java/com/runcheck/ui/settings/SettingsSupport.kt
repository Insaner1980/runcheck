package com.runcheck.ui.settings

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
import androidx.compose.ui.platform.LocalContext
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
    actions: SettingsTransientEffectActions,
) {
    val context = LocalContext.current
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
            if (!shareExportUris(context, exportUriStrings)) {
                Toast.makeText(context, R.string.settings_export_error, Toast.LENGTH_SHORT).show()
            }
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
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_reset_thresholds_confirm_title),
            message = stringResource(R.string.settings_reset_thresholds_confirm_message),
            confirmLabel = stringResource(R.string.settings_reset_thresholds),
            onConfirm = {
                handles.showResetThresholdsDialog.value = false
                actions.onConfirmResetThresholds()
            },
            onDismiss = { handles.showResetThresholdsDialog.value = false },
        )
    }

    if (handles.showResetTipsDialog.value) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_reset_tips_confirm_title),
            message = stringResource(R.string.settings_reset_tips_confirm_message),
            confirmLabel = stringResource(R.string.settings_reset_tips),
            onConfirm = {
                handles.showResetTipsDialog.value = false
                actions.onConfirmResetTips()
            },
            onDismiss = { handles.showResetTipsDialog.value = false },
        )
    }

    if (handles.showClearSpeedTestsDialog.value) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_clear_speed_tests_confirm_title),
            message = stringResource(R.string.settings_clear_speed_tests_confirm_message),
            confirmLabel = stringResource(R.string.settings_clear_action),
            onConfirm = {
                handles.showClearSpeedTestsDialog.value = false
                actions.onConfirmClearSpeedTests()
            },
            onDismiss = { handles.showClearSpeedTestsDialog.value = false },
        )
    }

    if (handles.showNotifPermissionDeniedDialog.value) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.notification_permission_denied_title),
            message = stringResource(R.string.notification_permission_denied_message),
            confirmLabel = stringResource(R.string.notification_permission_denied_open_settings),
            onConfirm = {
                handles.showNotifPermissionDeniedDialog.value = false
                actions.onOpenNotificationSettings()
            },
            onDismiss = { handles.showNotifPermissionDeniedDialog.value = false },
        )
    }

    if (handles.showClearDialog.value) {
        SettingsConfirmationDialog(
            title = stringResource(R.string.settings_clear_confirm_title),
            message = stringResource(R.string.settings_clear_confirm_message),
            confirmLabel = stringResource(R.string.settings_clear_action),
            onConfirm = {
                handles.showClearDialog.value = false
                actions.onConfirmClearDialog()
            },
            onDismiss = { handles.showClearDialog.value = false },
        )
    }
}

@Composable
private fun SettingsConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}
