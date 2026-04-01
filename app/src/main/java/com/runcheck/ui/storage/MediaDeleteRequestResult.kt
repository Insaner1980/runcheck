package com.runcheck.ui.storage

import android.content.Context
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.StringRes
import com.runcheck.R
import com.runcheck.data.storage.StorageCleanupHelper

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
): MediaDeleteRequestResult {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uriStrings.isEmpty()) {
        return MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed)
    }
    return buildMediaDeleteRequestResult(
        sdkInt = Build.VERSION.SDK_INT,
        uriStrings = uriStrings,
    ) { deleteUris ->
        createDeleteRequest(context, deleteUris)
    }
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
    } catch (_: SecurityException) {
        MediaDeleteRequestResult.Failed(R.string.cleanup_delete_permission_error)
    } catch (_: IllegalArgumentException) {
        MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed)
    }
}

private fun createDeleteRequest(
    context: Context,
    uriStrings: List<String>,
): IntentSenderRequest {
    val deleteRequest =
        StorageCleanupHelper(context).createDeleteRequest(uriStrings)
            ?: throw IllegalArgumentException("Delete request unavailable")
    return IntentSenderRequest.Builder(deleteRequest.intentSender).build()
}
