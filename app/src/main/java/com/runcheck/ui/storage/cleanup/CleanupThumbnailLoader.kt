package com.runcheck.ui.storage.cleanup

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object CleanupThumbnailCache {
    val thumbnails = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    private const val MAX_CACHE_SIZE_KB = 6 * 1024
}

suspend fun loadCleanupThumbnail(
    context: Context,
    uriString: String,
    sizePx: Int = 96
): Bitmap? = withContext(Dispatchers.IO) {
    val cacheKey = "$uriString@$sizePx"
    CleanupThumbnailCache.thumbnails.get(cacheKey)?.let { return@withContext it }

    try {
        val uri = Uri.parse(uriString)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
        } else {
            loadLegacyThumbnail(context, uri)
        }
        bitmap?.also { CleanupThumbnailCache.thumbnails.put(cacheKey, it) }
    } catch (_: Exception) {
        null
    }
}

@Suppress("DEPRECATION")
private fun loadLegacyThumbnail(context: Context, uri: Uri): Bitmap? {
    val id = ContentUris.parseId(uri)
    val path = uri.path.orEmpty()
    return when {
        "/images/" in path -> MediaStore.Images.Thumbnails.getThumbnail(
            context.contentResolver,
            id,
            MediaStore.Images.Thumbnails.MINI_KIND,
            null
        )

        "/video/" in path -> MediaStore.Video.Thumbnails.getThumbnail(
            context.contentResolver,
            id,
            MediaStore.Video.Thumbnails.MINI_KIND,
            null
        )

        else -> null
    }
}
