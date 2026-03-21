package com.runcheck.ui.storage

import android.app.RemoteAction
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import androidx.activity.result.IntentSenderRequest
import com.runcheck.R
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageDeleteRequestTest {

    @Test
    fun `returns failed when sdk is below api 30`() {
        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.Q,
            uriStrings = listOf("content://media/1")
        ) { error("should not be called") }

        assertEquals(
            MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed),
            result
        )
    }

    @Test
    fun `returns failed when uri list is empty`() {
        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.R,
            uriStrings = emptyList()
        ) { error("should not be called") }

        assertEquals(
            MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed),
            result
        )
    }

    @Test
    fun `returns ready when request factory succeeds`() {
        val request = IntentSenderRequest.Builder(mockk<IntentSender>()).build()

        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.R,
            uriStrings = listOf("content://media/1")
        ) { request }

        assertTrue(result is MediaDeleteRequestResult.Ready)
        assertEquals(request, (result as MediaDeleteRequestResult.Ready).request)
    }

    @Test
    fun `security exception maps to permission error`() {
        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.R,
            uriStrings = listOf("content://media/1")
        ) {
            throw SecurityException("denied")
        }

        assertEquals(
            MediaDeleteRequestResult.Failed(R.string.cleanup_delete_permission_error),
            result
        )
    }

    @Test
    fun `recoverable security exception maps to permission error`() {
        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.R,
            uriStrings = listOf("content://media/1")
        ) {
            throw RecoverableSecurityException(
                SecurityException("recoverable"),
                "Recoverable",
                mockk<RemoteAction>(relaxed = true)
            )
        }

        assertEquals(
            MediaDeleteRequestResult.Failed(R.string.cleanup_delete_permission_error),
            result
        )
    }

    @Test
    fun `illegal argument maps to generic delete failure`() {
        val result = buildMediaDeleteRequestResult(
            sdkInt = Build.VERSION_CODES.R,
            uriStrings = listOf("content://media/1")
        ) {
            throw IllegalArgumentException("bad uri")
        }

        assertEquals(
            MediaDeleteRequestResult.Failed(R.string.cleanup_delete_failed),
            result
        )
    }
}
