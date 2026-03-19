package com.runcheck.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var monitorScheduler: MonitoringScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                monitorScheduler.ensureScheduled()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                ReleaseSafeLog.error(TAG, "Failed to reschedule monitoring after boot", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        private const val TAG = "BootReceiver"
    }
}
