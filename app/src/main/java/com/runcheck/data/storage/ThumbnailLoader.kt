package com.runcheck.data.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val cache = LruCache<Uri, ImageBitmap>(50)

    suspend fun loadThumbnail(uri: Uri, sizePx: Int = 96): ImageBitmap? =
        withContext(Dispatchers.IO) {
            cache.get(uri)?.let { return@withContext it }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
            try {
                val bitmap = context.contentResolver.loadThumbnail(
                    uri, android.util.Size(sizePx, sizePx), null
                )
                val imageBitmap = bitmap.asImageBitmap()
                cache.put(uri, imageBitmap)
                imageBitmap
            } catch (_: Exception) { null }
        }
}
