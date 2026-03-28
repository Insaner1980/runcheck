package com.runcheck.domain.model

data class ScannedFile(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateModified: Long,
    val category: MediaCategory,
)
