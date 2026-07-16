package com.runcheck.data.storage

import com.runcheck.ui.storage.cleanup.CleanupType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupFilterSelectionTest {
    @Test
    fun `large file filters use exact SI megabyte boundaries`() {
        assertEquals(
            listOf(10L, 50L, 100L, 500L).map { it * 1_000_000 },
            CleanupType.LARGE_FILES.filterOptions.map { it.value },
        )
        assertEquals(1, CleanupType.LARGE_FILES.defaultFilterIndex)
    }

    @Test
    fun `large file selection excludes a file exactly on the chosen boundary`() {
        val thresholdBytes = 50L * 1_000_000

        val selection = largeFileSelection(thresholdBytes)

        assertEquals("_size > ?", selection.sql)
        assertArrayEquals(arrayOf(thresholdBytes.toString()), selection.args)
    }

    @Test
    fun `old download filters use exact day and one year boundaries`() {
        assertEquals(
            listOf(30L, 60L, 90L, 365L).map { it * 86_400_000 },
            CleanupType.OLD_DOWNLOADS.filterOptions.map { it.value },
        )
        assertEquals(0, CleanupType.OLD_DOWNLOADS.defaultFilterIndex)
    }

    @Test
    fun `old download selection uses an exclusive fixed scan cutoff in seconds`() {
        val thirtyDays = 30L * 86_400_000
        val startedAtMillis = 1_800_000_000_999L

        val selection = oldDownloadSelection(thirtyDays, startedAtMillis)

        assertEquals("date_modified < ?", selection.sql)
        assertArrayEquals(
            arrayOf(((startedAtMillis - thirtyDays) / 1000).toString()),
            selection.args,
        )
    }

    @Test
    fun `only APK cleanup preselects all results`() {
        assertFalse(CleanupType.LARGE_FILES.preselectAll)
        assertFalse(CleanupType.OLD_DOWNLOADS.preselectAll)
        assertTrue(CleanupType.APK_FILES.preselectAll)
    }

    @Test
    fun `APK selection accepts extension or package archive MIME`() {
        val selection = apkFileSelection(excludeDownloads = false)

        assertEquals("_display_name LIKE ? OR mime_type = ?", selection.sql)
        assertArrayEquals(
            arrayOf("%.apk", "application/vnd.android.package-archive"),
            selection.args,
        )
    }

    @Test
    fun `Files APK query excludes rows already exposed by Downloads`() {
        val selection = apkFileSelection(excludeDownloads = true)

        assertEquals(
            "(_display_name LIKE ? OR mime_type = ?) AND (is_download IS NULL OR is_download != 1)",
            selection.sql,
        )
        assertArrayEquals(
            arrayOf("%.apk", "application/vnd.android.package-archive"),
            selection.args,
        )
    }
}
