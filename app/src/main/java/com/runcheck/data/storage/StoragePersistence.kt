package com.runcheck.data.storage

internal fun decodePersistedStorageBytes(value: Long): Long? = value.takeIf { it >= 0L }
