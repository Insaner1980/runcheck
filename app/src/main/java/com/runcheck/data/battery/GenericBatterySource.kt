package com.runcheck.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.runcheck.data.device.DeviceCapabilityManager
import com.runcheck.data.device.DeviceProfile
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.CurrentUnit
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignConvention
import com.runcheck.util.ReleaseSafeLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlin.math.abs

open class GenericBatterySource(
    protected val context: Context,
    protected val profile: DeviceProfile
) : BatteryDataSource {

    protected val batteryManager: BatteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val sourceJob = SupervisorJob()
    private val sourceScope = CoroutineScope(
        sourceJob + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            ReleaseSafeLog.error(TAG, "Battery source flow failed", throwable)
        }
    )

    fun close() {
        sourceJob.cancel()
    }
    private val batteryChangedSharedFlow: Flow<Intent> by lazy {
        batteryChangedFlow().shareIn(
            scope = sourceScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 0),
            replay = 1
        )
    }

    protected fun batteryChangedFlow(): Flow<Intent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(intent)
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    override fun getCurrentNow(): Flow<MeasuredValue<Int>> = flow {
        while (true) {
            val rawCurrent = readCurrentNowRaw()
            if (rawCurrent == null) {
                emit(unavailableCurrent())
                delay(POLLING_INTERVAL_MS)
                continue
            }
            val currentMa = alignCurrentSignWithChargeState(normalizeCurrent(rawCurrent))
            val confidence = calculateCurrentConfidence(rawCurrent)

            emit(MeasuredValue(currentMa, confidence))
            delay(POLLING_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    protected fun unavailableCurrent(): MeasuredValue<Int> = MeasuredValue(
        value = 0,
        confidence = Confidence.UNAVAILABLE
    )

    protected fun readCurrentNowRaw(): Int? = try {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            .takeUnless { it == Int.MIN_VALUE }
    } catch (_: Exception) {
        null
    }

    protected fun normalizeCurrent(raw: Int): Int {
        val milliamps = when (resolveCurrentUnit(raw)) {
            CurrentUnit.MICROAMPS -> raw / 1000
            CurrentUnit.MILLIAMPS -> raw
        }
        return when (profile.currentNowSignConvention) {
            SignConvention.POSITIVE_CHARGING -> milliamps
            SignConvention.NEGATIVE_CHARGING -> -milliamps
        }
    }

    protected fun alignCurrentSignWithChargeState(currentMa: Int): Int = when {
        batteryManager.isCharging && currentMa < 0 -> abs(currentMa)
        !batteryManager.isCharging && currentMa > 0 -> -abs(currentMa)
        else -> currentMa
    }

    protected fun calculateCurrentConfidence(rawCurrent: Int): Confidence = when {
        rawCurrent == 0 -> Confidence.UNAVAILABLE
        !profile.currentNowReliable -> Confidence.LOW
        else -> Confidence.HIGH
    }

    private fun resolveCurrentUnit(raw: Int): CurrentUnit {
        if (profile.currentNowUnit == CurrentUnit.MICROAMPS) {
            return CurrentUnit.MICROAMPS
        }

        // Capability detection happens once and can be ambiguous when the first
        // samples are near zero. Re-check obviously large readings at runtime so
        // high-current µA devices do not get stuck with 1000x inflated values.
        return if (abs(raw) > DeviceCapabilityManager.MICROAMP_THRESHOLD) {
            CurrentUnit.MICROAMPS
        } else {
            CurrentUnit.MILLIAMPS
        }
    }

    override fun getVoltage(): Flow<Int> =
        batteryChangedSharedFlow.map { intent ->
            intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        }

    override fun getTemperature(): Flow<Float> =
        batteryChangedSharedFlow.map { intent ->
            intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        }

    override fun getHealth(): Flow<BatteryHealth> =
        batteryChangedSharedFlow.map { intent ->
            val healthInt = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)
            mapHealth(healthInt)
        }

    override fun getCycleCount(): Flow<Int?> = flow {
        emit(null)
    }

    override fun getHealthPercent(): Flow<Int?> = flow {
        emit(null)
    }

    override fun getChargingStatus(): Flow<ChargingStatus> =
        batteryChangedSharedFlow.map { intent ->
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0)
            mapChargingStatus(status)
        }

    override fun getPlugType(): Flow<PlugType> =
        batteryChangedSharedFlow.map { intent ->
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            mapPlugType(plugged)
        }

    override fun getLevel(): Flow<Int> =
        batteryChangedSharedFlow.map { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (scale > 0) (level * 100) / scale else 0
        }

    override fun getTechnology(): Flow<String> =
        batteryChangedSharedFlow.map { intent ->
            intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty()
        }

    override fun getChargeCounter(): Flow<Int?> = flow {
        while (true) {
            val raw = try {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
                    .takeUnless { it == Int.MIN_VALUE || it == 0 }
            } catch (_: Exception) {
                null
            }
            emit(raw?.let { it / 1000 }) // µAh → mAh
            delay(POLLING_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    protected fun mapHealth(health: Int): BatteryHealth = when (health) {
        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.GOOD
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.OVERHEAT
        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.DEAD
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OVER_VOLTAGE
        BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.COLD
        else -> BatteryHealth.UNKNOWN
    }

    protected fun mapChargingStatus(status: Int): ChargingStatus = when (status) {
        BatteryManager.BATTERY_STATUS_CHARGING -> ChargingStatus.CHARGING
        BatteryManager.BATTERY_STATUS_DISCHARGING -> ChargingStatus.DISCHARGING
        BatteryManager.BATTERY_STATUS_FULL -> ChargingStatus.FULL
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> ChargingStatus.NOT_CHARGING
        else -> ChargingStatus.NOT_CHARGING
    }

    protected fun mapPlugType(plugged: Int): PlugType = when (plugged) {
        BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
        BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
        else -> PlugType.NONE
    }

    companion object {
        private const val TAG = "GenericBatterySource"
        protected const val POLLING_INTERVAL_MS = 2000L
    }
}
