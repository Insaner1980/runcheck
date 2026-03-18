package com.runcheck.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<String, Bitmap>(50)

    suspend fun loadThumbnail(uriString: String, sizePx: Int = 96): Bitmap? =
        withContext(Dispatchers.IO) {
            cache.get(uriString)?.let { return@withContext it }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
            try {
                val uri = Uri.parse(uriString)
                val bitmap = context.contentResolver.loadThumbnail(
                    uri, android.util.Size(sizePx, sizePx), null
                )
                cache.put(uriString, bitmap)
                bitmap
            } catch (_: Exception) { null }
        }
}
