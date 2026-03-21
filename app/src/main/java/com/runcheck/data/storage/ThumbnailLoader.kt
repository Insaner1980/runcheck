package com.runcheck.data.storage

import android.content.Context
import android.content.ContentUris
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailLoader @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE_KB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            value.byteCount / 1024
    }

    suspend fun loadThumbnail(uriString: String, sizePx: Int = 96): Bitmap? =
        withContext(Dispatchers.IO) {
            val cacheKey = "$uriString@$sizePx"
            cache.get(cacheKey)?.let { return@withContext it }
            try {
                val uri = Uri.parse(uriString)
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        uri,
                        android.util.Size(sizePx, sizePx),
                        null
                    )
                } else {
                    loadLegacyThumbnail(uri)
                }
                bitmap?.also { cache.put(cacheKey, it) }
            } catch (_: Exception) { null }
        }

    @Suppress("DEPRECATION")
    private fun loadLegacyThumbnail(uri: Uri): Bitmap? {
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

    private companion object {
        private const val MAX_CACHE_SIZE_KB = 6 * 1024
    }
}
