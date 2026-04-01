package com.runcheck.data.storage

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.runcheck.domain.model.StorageDeleteFailure
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupHelper
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        /**
         * API 30+: Creates a PendingIntent that shows the system confirmation dialog
         * for batch deletion. Returns null on older API levels.
         */
        fun createDeleteRequest(uriStrings: List<String>): PendingIntent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriStrings.isEmpty()) return null
            val uris = uriStrings.map { Uri.parse(it) }
            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }

        /**
         * API 29 and below: Deletes files one by one without system dialog.
         * Returns the set of URIs successfully deleted.
         *
         * On API 29 (scoped storage without createDeleteRequest), non-owned files
         * throw RecoverableSecurityException. These are skipped and deletion
         * continues with remaining files — the caller handles partial results.
         */
        suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String> =
            withContext(Dispatchers.IO) {
                val deletedUris = mutableSetOf<String>()
                var skippedCount = 0
                uriStrings.forEach { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        if (context.contentResolver.delete(uri, null, null) > 0) {
                            deletedUris += uriString
                        }
                    } catch (error: SecurityException) {
                        skippedCount++
                        val message =
                            if (isRecoverableDeleteSecurityException(error)) {
                                "Skipping non-owned file (API 29 scoped storage)"
                            } else {
                                "Skipping file due to security restriction"
                            }
                        ReleaseSafeLog.warn(TAG, message, error)
                    }
                }
                if (deletedUris.isEmpty() && skippedCount > 0) {
                    throw StorageDeleteFailure(
                        deletedUris = emptySet(),
                        recoverable = true,
                    )
                }
                deletedUris
            }

        private companion object {
            private const val TAG = "StorageCleanupHelper"
            private const val RECOVERABLE_SECURITY_EXCEPTION =
                "android.app.RecoverableSecurityException"
        }

        private fun isRecoverableDeleteSecurityException(error: SecurityException): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                error.javaClass.name == RECOVERABLE_SECURITY_EXCEPTION
    }
