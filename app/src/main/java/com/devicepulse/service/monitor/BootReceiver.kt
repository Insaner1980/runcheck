package com.devicepulse.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devicepulse.domain.repository.MonitoringScheduler
import dagger.hilt.android.AndroidEntryPoint
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
            } finally {
                pendingResult.finish()
            }
        }
    }
}
