package com.runcheck.domain.model

data class MediaBreakdown(
    val imagesBytes: Long,
    val videosBytes: Long,
    val audioBytes: Long,
    val documentsBytes: Long,
    val downloadsBytes: Long,
)
