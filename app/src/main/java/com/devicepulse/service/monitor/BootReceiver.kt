package com.devicepulse.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devicepulse.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var monitorScheduler: MonitorScheduler

    @Inject
    lateinit var preferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = preferencesRepository.getPreferences().first()
                monitorScheduler.schedule(prefs.monitoringInterval)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
