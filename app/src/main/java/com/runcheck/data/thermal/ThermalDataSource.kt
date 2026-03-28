package com.runcheck.data.thermal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.runcheck.domain.model.ThermalStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalDataSource
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        private val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager

        fun getBatteryTemperature(): Flow<Float> =
            callbackFlow {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            context: Context,
                            intent: Intent,
                        ) {
                            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                            trySend(temp)
                        }
                    }
                val stickyIntent =
                    ContextCompat.registerReceiver(
                        context,
                        receiver,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                        ContextCompat.RECEIVER_NOT_EXPORTED,
                    )
                stickyIntent?.let { receiver.onReceive(context, it) }
                awaitClose { context.unregisterReceiver(receiver) }
            }

        fun getCpuTemperature(_unusedThermalZones: List<String>): Flow<Float?> =
            flow {
                emit(null)
            }

        fun getThermalHeadroom(): Flow<Float?> =
            flow {
                if (supportsThermalHeadroom(Build.VERSION.SDK_INT)) {
                    while (true) {
                        val headroom =
                            try {
                                val value = powerManager.getThermalHeadroom(HEADROOM_FORECAST_SECONDS)
                                if (value.isNaN() || value < 0f) null else value
                            } catch (_: Exception) {
                                null
                            }
                        emit(headroom)
                        delay(POLLING_INTERVAL_MS)
                    }
                } else {
                    emit(null)
                }
            }.flowOn(Dispatchers.IO)

        fun getThermalStatus(): Flow<ThermalStatus> =
            flow {
                if (!supportsThermalStatus(Build.VERSION.SDK_INT)) {
                    emit(ThermalStatus.NONE)
                    return@flow
                }

                emitAllThermalStatus()
            }

        private fun mapThermalStatus(status: Int): ThermalStatus = mapThermalStatus(status, Build.VERSION.SDK_INT)

        companion object {
            private const val POLLING_INTERVAL_MS = 3000L
            private const val HEADROOM_FORECAST_SECONDS = 10
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private suspend fun kotlinx.coroutines.flow.FlowCollector<ThermalStatus>.emitAllThermalStatus() {
            callbackFlow {
                trySend(mapThermalStatus(powerManager.currentThermalStatus))

                val listener =
                    PowerManager.OnThermalStatusChangedListener { status ->
                        trySend(mapThermalStatus(status))
                    }

                powerManager.addThermalStatusListener(
                    ContextCompat.getMainExecutor(context),
                    listener,
                )

                awaitClose {
                    powerManager.removeThermalStatusListener(listener)
                }
            }.collect { emit(it) }
        }
    }

internal fun supportsThermalStatus(apiLevel: Int): Boolean = apiLevel >= Build.VERSION_CODES.Q

internal fun supportsThermalHeadroom(apiLevel: Int): Boolean = apiLevel >= Build.VERSION_CODES.R

internal fun mapThermalStatus(
    status: Int,
    apiLevel: Int,
): ThermalStatus {
    if (!supportsThermalStatus(apiLevel)) return ThermalStatus.NONE
    return when (status) {
        PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
        PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
        PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatus.CRITICAL
        PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatus.EMERGENCY
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.SHUTDOWN
        else -> ThermalStatus.NONE
    }
}
