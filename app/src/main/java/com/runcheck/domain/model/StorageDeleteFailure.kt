package com.runcheck.domain.model

class StorageDeleteFailure(
    val deletedUris: Set<String>,
    val recoverable: Boolean,
) : RuntimeException()
