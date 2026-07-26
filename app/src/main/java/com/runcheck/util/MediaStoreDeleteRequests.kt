package com.runcheck.util

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri

fun createMediaStoreDeleteRequest(
    context: Context,
    uriStrings: List<String>,
): PendingIntent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriStrings.isEmpty()) return null
    val uris = uriStrings.map { it.toUri() }
    return MediaStore.createDeleteRequest(context.contentResolver, uris)
}
