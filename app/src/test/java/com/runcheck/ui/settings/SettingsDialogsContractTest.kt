package com.runcheck.ui.settings

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class SettingsDialogsContractTest {
    private val settingsRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui/settings")
    private val source = settingsRoot.resolve("SettingsSupport.kt").readText()

    private val expected =
        listOf(
            listOf(
                "showResetThresholdsDialog",
                "settings_reset_thresholds_confirm_title",
                "settings_reset_thresholds_confirm_message",
                "settings_reset_thresholds",
                "onConfirmResetThresholds",
            ),
            listOf(
                "showResetTipsDialog",
                "settings_reset_tips_confirm_title",
                "settings_reset_tips_confirm_message",
                "settings_reset_tips",
                "onConfirmResetTips",
            ),
            listOf(
                "showClearSpeedTestsDialog",
                "settings_clear_speed_tests_confirm_title",
                "settings_clear_speed_tests_confirm_message",
                "settings_clear_action",
                "onConfirmClearSpeedTests",
            ),
            listOf(
                "showNotifPermissionDeniedDialog",
                "notification_permission_denied_title",
                "notification_permission_denied_message",
                "notification_permission_denied_open_settings",
                "onOpenNotificationSettings",
            ),
            listOf(
                "showClearDialog",
                "settings_clear_confirm_title",
                "settings_clear_confirm_message",
                "settings_clear_action",
                "onConfirmClearDialog",
            ),
        )

    @Test
    fun `all five callers preserve text visibility dismiss and confirm ordering`() {
        val body =
            source
                .substringAfter("actions: SettingsDialogActions,")
                .substringAfter(") {")
                .substringBefore("@Composable")
                .normalized()
        val expectedBody =
            expected.joinToString(" ") { values ->
                val handle = values[0]
                val title = values[1]
                val message = values[2]
                val label = values[3]
                val action = values[4]
                """
            if (handles.$handle.value) {
                SettingsConfirmationDialog(
                    title = stringResource(R.string.$title),
                    message = stringResource(R.string.$message),
                    confirmLabel = stringResource(R.string.$label),
                    onConfirm = {
                        handles.$handle.value = false
                        actions.$action()
                    },
                    onDismiss = { handles.$handle.value = false },
                )
            }
            """.normalized()
            }
        assertEquals("$expectedBody }", body)
    }

    @Test
    fun `shell preserves material presentation and routes cancel and system dismissal identically`() {
        val body = source.substringAfter("private fun SettingsConfirmationDialog(").substringAfter(") {")
        assertEquals(
            """
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
            """.normalized(),
            body.normalized(),
        )
        assertEquals(1, Regex("\\bAlertDialog\\(").findAll(source).count())
    }

    @Test
    fun `screen connects each confirmation to its original operation`() {
        val screen = settingsRoot.resolve("SettingsScreen.kt").readText().normalized()
        listOf(
            "onConfirmResetThresholds = { viewModel.resetAlertThresholds() }",
            "onConfirmResetTips = { viewModel.resetTips() }",
            "onConfirmClearSpeedTests = { viewModel.clearSpeedTests() }",
            "onOpenNotificationSettings = { openSystemNotificationSettings(context) }",
            "onConfirmClearDialog = { viewModel.clearAllData() }",
        ).forEach { wiring -> assertTrue(wiring, screen.contains(wiring)) }
    }

    private fun String.normalized(): String = replace(Regex("\\s+"), " ").trim()
}
