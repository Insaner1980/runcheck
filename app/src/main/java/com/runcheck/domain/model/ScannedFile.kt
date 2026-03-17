package com.runcheck.domain.model

import android.net.Uri

data class ScannedFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateModified: Long,
    val category: MediaCategory
)
