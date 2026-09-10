package com.runcheck.ui.common

import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// UI-triggered persistence must not clear visible state when an implementation-specific write fails.
@Suppress("TooGenericExceptionCaught")
internal fun CoroutineScope.launchUiMutation(
    tag: String,
    action: String,
    mutation: suspend () -> Unit,
) {
    launch {
        try {
            mutation()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            ReleaseSafeLog.error(tag, "Failed to $action", error)
        }
    }
}
