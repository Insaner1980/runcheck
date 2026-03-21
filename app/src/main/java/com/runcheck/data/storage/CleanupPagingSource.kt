package com.runcheck.data.storage

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.runcheck.domain.model.ScannedFile

internal class CleanupPagingSource(
    private val loader: suspend (offset: Int, limit: Int) -> List<ScannedFile>
) : PagingSource<Int, ScannedFile>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ScannedFile> {
        val offset = params.key ?: 0
        return try {
            val items = loader(offset, params.loadSize)
            val nextKey = if (items.size < params.loadSize) null else offset + items.size
            LoadResult.Page(
                data = items,
                prevKey = if (offset == 0) null else (offset - params.loadSize).coerceAtLeast(0),
                nextKey = nextKey
            )
        } catch (t: Throwable) {
            LoadResult.Error(t)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ScannedFile>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(state.config.pageSize)
            ?: anchorPage.nextKey?.minus(state.config.pageSize)
    }
}
