package com.runcheck.service.monitor

import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught")
internal suspend fun collectWorkerStep(
    tag: String,
    workflowName: String,
    stepName: String,
    block: suspend () -> Unit,
): Boolean =
    try {
        block()
        false
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ReleaseSafeLog.error(tag, "$workflowName step failed: $stepName", e)
        true
    }
