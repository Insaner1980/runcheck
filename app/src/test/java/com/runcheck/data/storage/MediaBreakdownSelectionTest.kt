package com.runcheck.data.storage

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaBreakdownSelectionTest {
    @Test
    fun `media category excludes files in Download directory`() {
        val selection = mediaBreakdownSelection(downloadDirectory = "Download")

        assertEquals("relative_path IS NULL OR relative_path NOT LIKE ?", selection.sql)
        assertArrayEquals(arrayOf("Download/%"), selection.args)
    }

    @Test
    fun `document category combines MIME and non-Download filters`() {
        val selection =
            mediaBreakdownSelection(
                mimePattern = "application/pdf",
                downloadDirectory = "Download",
            )

        assertEquals(
            "mime_type LIKE ? AND (relative_path IS NULL OR relative_path NOT LIKE ?)",
            selection.sql,
        )
        assertArrayEquals(arrayOf("application/pdf", "Download/%"), selection.args)
    }

    @Test
    fun `selection equality compares argument content`() {
        val first = MediaBreakdownSelection("size > ?", arrayOf("100"))
        val sameContent = MediaBreakdownSelection("size > ?", arrayOf("100"))

        assertEquals(first, sameContent)
        assertEquals(first.hashCode(), sameContent.hashCode())
    }
}
