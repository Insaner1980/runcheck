package com.runcheck.service.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.runcheck.domain.repository.ScreenStateRepository
import com.runcheck.util.ReleaseSafeLog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScreenStateReceiver : BroadcastReceiver() {
    @Inject
    lateinit var screenStateRepository: ScreenStateRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    Intent.ACTION_SCREEN_ON -> screenStateRepository.onScreenTurnedOn()
                    Intent.ACTION_SCREEN_OFF -> screenStateRepository.onScreenTurnedOff()
                    Intent.ACTION_POWER_CONNECTED -> screenStateRepository.onPowerConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> screenStateRepository.onPowerDisconnected()
                    PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> screenStateRepository.onDeviceIdleModeChanged()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ReleaseSafeLog.error(TAG, "Failed to handle screen state action: $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        private const val TAG = "ScreenStateReceiver"
    }
}
