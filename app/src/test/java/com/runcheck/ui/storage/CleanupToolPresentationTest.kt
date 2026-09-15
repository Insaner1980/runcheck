package com.runcheck.ui.storage

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhoneAndroid
import com.runcheck.R
import com.runcheck.testutil.findRootDir
import com.runcheck.ui.storage.cleanup.CleanupType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.readText

class CleanupToolPresentationTest {
    @Test
    fun `canonical entry order covers every cleanup destination with separate entry presentation`() {
        val entries = CleanupType.entries.map { it to it.toolPresentation() }
        assertEquals(
            listOf(CleanupType.LARGE_FILES, CleanupType.OLD_DOWNLOADS, CleanupType.APK_FILES),
            entries.map { it.first },
        )
        assertEquals(
            listOf(
                CleanupToolPresentation(
                    R.string.storage_large_files,
                    R.string.storage_large_files_desc,
                    Icons.Outlined.FolderOpen,
                    CleanupToolTint.POOR,
                ),
                CleanupToolPresentation(
                    R.string.storage_old_downloads,
                    R.string.storage_old_downloads_desc,
                    Icons.Outlined.Download,
                    CleanupToolTint.PRIMARY,
                ),
                CleanupToolPresentation(
                    R.string.storage_apk_files,
                    R.string.storage_apk_files_desc,
                    Icons.Outlined.PhoneAndroid,
                    CleanupToolTint.APK,
                ),
            ),
            entries.map { it.second },
        )
        entries.forEach { (type, presentation) -> assertTrue(type.titleRes != presentation.titleRes) }
    }

    @Test
    fun `storage renders canonical entries and forwards the same type to navigation`() {
        val source =
            findRootDir()
                .resolve("app/src/main/java/com/runcheck/ui/storage/StorageDetailSupport.kt")
                .readText()
                .replace(Regex("\\s+"), " ")
        assertTrue(
            source.contains(
                "CleanupType.entries.forEach { type -> val presentation = type.toolPresentation() " +
                    "ActionCard( icon = presentation.icon, iconTint = presentation.tint.color(), " +
                    "title = stringResource(presentation.titleRes), " +
                    "subtitle = stringResource(presentation.descriptionRes), " +
                    "actionLabel = stringResource(R.string.storage_scan), onAction = { onNavigateToCleanup(type) }, ) }",
            ),
        )
        assertTrue(source.contains("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { storage.trashInfo?.let"))
    }
}
