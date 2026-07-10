package com.runcheck.data.storage

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.runcheck.domain.model.ScannedFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupPagingSourceTest {
    @Test
    fun `load forwards the requested offset and load size`() =
        runTest {
            var request: Pair<Int, Int>? = null
            val source =
                CleanupPagingSource(
                    loader = { offset, limit ->
                        request = offset to limit
                        emptyList()
                    },
                    registerInvalidation = { {} },
                )

            source.load(
                PagingSource.LoadParams.Refresh(
                    key = null,
                    loadSize = 120,
                    placeholdersEnabled = false,
                ),
            )

            assertEquals(0 to 120, request)
        }

    @Test
    fun `data change invalidates the source and unregisters the observer`() {
        var notifyChange: (() -> Unit)? = null
        var observerUnregistered = false
        val source =
            CleanupPagingSource(
                loader = { _, _ -> emptyList() },
                registerInvalidation = { onChange ->
                    notifyChange = onChange
                    { observerUnregistered = true }
                },
            )

        notifyChange?.invoke()

        assertTrue(source.invalid)
        assertTrue(observerUnregistered)
    }

    @Test
    fun `refresh key centers the default three-page initial load around anchor`() {
        val source =
            CleanupPagingSource(
                loader = { _, _ -> emptyList() },
                registerInvalidation = { {} },
            )
        val state =
            PagingState<Int, ScannedFile>(
                pages = emptyList(),
                anchorPosition = 100,
                config = PagingConfig(pageSize = 40),
                leadingPlaceholderCount = 0,
            )

        assertEquals(40, source.getRefreshKey(state))
    }
}
