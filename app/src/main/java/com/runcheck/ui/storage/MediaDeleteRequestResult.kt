package com.runcheck.ui.storage

import android.app.RecoverableSecurityException
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
import com.runcheck.R

sealed interface MediaDeleteRequestResult {
    data class Ready(
        val request: IntentSenderRequest,
    ) : MediaDeleteRequestResult

    data class Failed(
        @param:StringRes val messageRes: Int,
    ) : MediaDeleteRequestResult
}

fun buildMediaDeleteRequest(
    context: Context,
    uriStrings: List<String>,
): MediaDeleteRequestResult =
    buildMediaDeleteRequestResult(
        sdkInt = Build.VERSION.SDK_INT,
        uriStrings = uriStrings,
    ) { deleteUris ->
        val uris = deleteUris.map(android.net.Uri::parse)
        val deleteRequest = MediaStore.createDeleteRequest(context.contentResolver, uris)
        IntentSenderRequest.Builder(deleteRequest.intentSender).build()
    }

internal fun buildMediaDeleteRequestResult(
    sdkInt: Int,
    uriStrings: List<String>,
    requestFactory: (List<String>) -> IntentSenderRequest,
): MediaDeleteRequestResult {
    if (sdkInt < Build.VERSION_CODES.R || uriStrings.isEmpty()) {
        return MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed)
    }

    return try {
        MediaDeleteRequestResult.Ready(requestFactory(uriStrings))
    } catch (_: RecoverableSecurityException) {
        MediaDeleteRequestResult.Failed(R.string.cleanup_delete_permission_error)
    } catch (_: SecurityException) {
        MediaDeleteRequestResult.Failed(R.string.cleanup_delete_permission_error)
    } catch (_: IllegalArgumentException) {
        MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed)
    }
}
