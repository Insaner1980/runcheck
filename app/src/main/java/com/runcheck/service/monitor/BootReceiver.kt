package com.runcheck.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.runcheck.domain.repository.MonitoringScheduler
import com.runcheck.domain.repository.ScreenStateRepository
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

    @Inject
    lateinit var screenStateRepository: ScreenStateRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                screenStateRepository.initialize()
                monitorScheduler.ensureScheduled()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                ReleaseSafeLog.error(TAG, "Failed to reschedule monitoring after $action", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        private const val TAG = "BootReceiver"
    }
}
