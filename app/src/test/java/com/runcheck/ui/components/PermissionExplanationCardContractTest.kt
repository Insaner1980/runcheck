package com.runcheck.ui.components

import com.runcheck.testutil.findRootDir
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class PermissionExplanationCardContractTest {
    private val uiRoot = findRootDir().resolve("app/src/main/java/com/runcheck/ui")

    @Test
    fun `storage retains partial and settings copy selection at the call site`() {
        val source = source("storage/StorageDetailScreen.kt")
        val wrapper =
            source
                .substringAfter("private fun StorageMediaPermissionCard(")
                .substringBefore("// ── Hero card")

        assertTrue(wrapper.contains("PermissionExplanationCard("))
        assertTrue(
            wrapper.contains(
                "if (partialAccess) { R.string.storage_media_permission_partial_title } else { R.string.storage_media_permission_title }",
            ),
        )
        assertTrue(
            wrapper.contains(
                "if (partialAccess) { R.string.storage_media_permission_partial_message } else { R.string.storage_media_permission_message }",
            ),
        )
        assertTrue(
            wrapper.contains(
                "if (shouldOpenSettings) { R.string.storage_media_permission_open_settings } " +
                    "else if (partialAccess) { R.string.storage_media_permission_manage_selection } " +
                    "else { R.string.storage_media_permission_grant }",
            ),
        )
        assertTrue(wrapper.contains("onAction = onAction"))
        assertFalse(wrapper.contains("RuncheckCard("))
        assertTrue(source.contains("if (!hasAllMediaPermissions) { StorageMediaPermissionCard("))
    }

    @Test
    fun `usage access branch retains its resources and settings callback`() {
        val branch =
            source("appusage/AppUsageScreen.kt")
                .substringAfter("!hasUsageAccess ->")
                .substringBefore("appItems.loadState.refresh is LoadState.Loading")

        assertTrue(branch.contains("PermissionExplanationCard("))
        assertTrue(branch.contains("title = stringResource(R.string.app_usage_permission_title)"))
        assertTrue(branch.contains("message = stringResource(R.string.app_usage_permission_message)"))
        assertTrue(branch.contains("actionLabel = stringResource(R.string.app_usage_permission_open_settings)"))
        assertTrue(branch.contains("onAction = { context.openUsageAccessSettings() }"))
        assertFalse(branch.contains("RuncheckCard("))
    }

    private fun source(path: String): String = uiRoot.resolve(path).readText().replace(Regex("\\s+"), " ")
}
