package com.runcheck.ui.storage.cleanup

import com.runcheck.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupTypeTest {
    @Test
    fun `every cleanup type retains its exact product metadata and valid default`() {
        assertEquals(listOf("LARGE_FILES", "OLD_DOWNLOADS", "APK_FILES"), CleanupType.entries.map { it.name })
        CleanupType.entries.forEach { type ->
            val expected =
                when (type) {
                    CleanupType.LARGE_FILES -> {
                        ExpectedMetadata(
                            R.string.cleanup_large_files_title,
                            listOf(
                                FilterOption(R.string.cleanup_filter_10mb, 10_000_000L),
                                FilterOption(R.string.cleanup_filter_50mb, 50_000_000L),
                                FilterOption(R.string.cleanup_filter_100mb, 100_000_000L),
                                FilterOption(R.string.cleanup_filter_500mb, 500_000_000L),
                            ),
                            1,
                            false,
                            null,
                            50_000_000L,
                        )
                    }

                    CleanupType.OLD_DOWNLOADS -> {
                        ExpectedMetadata(
                            R.string.cleanup_old_downloads_title,
                            listOf(
                                FilterOption(R.string.cleanup_filter_30d, 2_592_000_000L),
                                FilterOption(R.string.cleanup_filter_60d, 5_184_000_000L),
                                FilterOption(R.string.cleanup_filter_90d, 7_776_000_000L),
                                FilterOption(R.string.cleanup_filter_1y, 31_536_000_000L),
                            ),
                            0,
                            false,
                            30,
                            2_592_000_000L,
                        )
                    }

                    CleanupType.APK_FILES -> {
                        ExpectedMetadata(R.string.cleanup_apk_files_title, emptyList(), 0, true, 30, 0L)
                    }
                }
            assertEquals(expected.title, type.titleRes)
            assertEquals(expected.filters, type.filterOptions)
            assertEquals(expected.defaultIndex, type.defaultFilterIndex)
            assertEquals(expected.preselect, type.preselectAll)
            assertEquals(expected.minimumApi, type.minimumSupportedApi)
            assertEquals(expected.queryDefault, type.filterOptions.getOrNull(type.defaultFilterIndex)?.value ?: 0L)
            if (type == CleanupType.APK_FILES) {
                assertTrue(type.filterOptions.isEmpty())
                assertEquals(0, type.defaultFilterIndex)
            } else {
                assertTrue(type.defaultFilterIndex in type.filterOptions.indices)
            }
        }
    }

    @Test
    fun `scan API policy preserves support below at and above Android 11`() {
        listOf(26, 29, 30, 37).forEach { api ->
            CleanupType.entries.forEach { type ->
                val expected = type == CleanupType.LARGE_FILES || api >= 30
                assertEquals("$type on API $api", expected, type.isSupportedOnApi(api))
            }
        }
    }

    private data class ExpectedMetadata(
        val title: Int,
        val filters: List<FilterOption>,
        val defaultIndex: Int,
        val preselect: Boolean,
        val minimumApi: Int?,
        val queryDefault: Long,
    )
}
