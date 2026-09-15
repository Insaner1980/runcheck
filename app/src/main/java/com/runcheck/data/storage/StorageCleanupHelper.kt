package com.runcheck.data.storage

import android.content.Context
import android.os.Build
import androidx.core.net.toUri
import com.runcheck.domain.model.StorageDeleteFailure
import com.runcheck.util.AppDispatchers
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageCleanupHelper
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) {
        /**
         * API 29 and below: Deletes files one by one without system dialog.
         * Returns the set of URIs successfully deleted.
         *
         * On API 29 (scoped storage without createDeleteRequest), a
         * RecoverableSecurityException is propagated so the UI can request
         * per-item user consent before retrying the deletion.
         */
        suspend fun deleteLegacy(uriStrings: Collection<String>): Set<String> =
            withContext(dispatchers.io) {
                val deletedUris = mutableSetOf<String>()
                var skippedCount = 0
                uriStrings.forEach { uriString ->
                    try {
                        val uri = uriString.toUri()
                        if (context.contentResolver.delete(uri, null, null) > 0) {
                            deletedUris += uriString
                        }
                    } catch (error: SecurityException) {
                        handleLegacyDeleteSecurityException(error)
                        skippedCount++
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

        companion object {
            private const val TAG = "StorageCleanupHelper"
            private const val RECOVERABLE_SECURITY_EXCEPTION =
                "android.app.RecoverableSecurityException"
        }

        private fun isRecoverableDeleteSecurityException(error: SecurityException): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                error.javaClass.name == RECOVERABLE_SECURITY_EXCEPTION

        private fun handleLegacyDeleteSecurityException(error: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                isRecoverableDeleteSecurityException(error)
            ) {
                throw error
            }
            val message =
                if (isRecoverableDeleteSecurityException(error)) {
                    "Skipping non-owned file (API 29 scoped storage)"
                } else {
                    "Skipping file due to security restriction"
                }
            ReleaseSafeLog.warn(TAG, message, error)
        }
    }
