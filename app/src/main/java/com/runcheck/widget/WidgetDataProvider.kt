package com.runcheck.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.runcheck.data.db.dao.BatteryReadingDao
import com.runcheck.data.db.dao.NetworkReadingDao
import com.runcheck.data.db.dao.StorageReadingDao
import com.runcheck.data.db.dao.ThermalReadingDao
import com.runcheck.data.db.entity.BatteryReadingEntity
import com.runcheck.data.db.entity.NetworkReadingEntity
import com.runcheck.data.db.entity.StorageReadingEntity
import com.runcheck.data.db.entity.ThermalReadingEntity
import com.runcheck.domain.model.BatteryHealth
import com.runcheck.domain.model.BatteryState
import com.runcheck.domain.model.ChargingStatus
import com.runcheck.domain.model.Confidence
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.MeasuredValue
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.SignalQuality
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.scoring.HealthScoreCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

internal data class BatteryWidgetSnapshot(
    val level: Int,
    val temperatureC: Float,
    val currentMa: Int?
)

internal data class HealthWidgetSnapshot(
    val overallScore: Int,
    val batteryLevel: Int
)

internal object WidgetDataProvider {

    fun isProUnlocked(context: Context): Boolean = entryPoint(context).proStatusProvider().isPro()

    suspend fun loadBatterySnapshot(context: Context): BatteryWidgetSnapshot? {
        val latestReading = entryPoint(context).batteryReadingDao().getLatestReading().first()
            ?: return null

        return BatteryWidgetSnapshot(
            level = latestReading.level,
            temperatureC = latestReading.temperatureC,
            currentMa = latestReading.currentMa
        )
    }

    suspend fun loadHealthSnapshot(context: Context): HealthWidgetSnapshot? {
        val entryPoint = entryPoint(context)
        val battery = entryPoint.batteryReadingDao().getLatestReading().first()?.toBatteryState()
            ?: return null
        val network = entryPoint.networkReadingDao().getLatestReading().first()?.toNetworkState()
            ?: DEFAULT_NETWORK
        val thermal = entryPoint.thermalReadingDao().getLatestReading().first()?.toThermalState()
            ?: DEFAULT_THERMAL
        val storage = entryPoint.storageReadingDao().getLatestReading().first()?.toStorageState()
            ?: DEFAULT_STORAGE

        val score = entryPoint.healthScoreCalculator().calculate(
            battery = battery,
            network = network,
            thermal = thermal,
            storage = storage
        )

        return HealthWidgetSnapshot(
            overallScore = score.overallScore,
            batteryLevel = battery.level
        )
    }

    private val DEFAULT_NETWORK = NetworkState(
        connectionType = ConnectionType.NONE,
        signalDbm = null,
        signalQuality = SignalQuality.NO_SIGNAL
    )

    private val DEFAULT_THERMAL = ThermalState(
        batteryTempC = 25f,
        thermalStatus = ThermalStatus.NONE,
        isThrottling = false
    )

    private val DEFAULT_STORAGE = StorageState(
        totalBytes = 1L,
        availableBytes = 1L,
        usedBytes = 0L,
        usagePercent = 0f
    )

    private fun entryPoint(context: Context): WidgetDataEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataEntryPoint::class.java
        )
}

internal object RuncheckWidgets {
    suspend fun updateAll(context: Context) {
        BatteryWidget().updateAll(context)
        HealthWidget().updateAll(context)
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetDataEntryPoint {
    fun batteryReadingDao(): BatteryReadingDao
    fun networkReadingDao(): NetworkReadingDao
    fun thermalReadingDao(): ThermalReadingDao
    fun storageReadingDao(): StorageReadingDao
    fun healthScoreCalculator(): HealthScoreCalculator
    fun proStatusProvider(): ProStatusProvider
}

private fun BatteryReadingEntity.toBatteryState(): BatteryState {
    val confidence = runCatching { Confidence.valueOf(currentConfidence) }
        .getOrDefault(Confidence.UNAVAILABLE)

    return BatteryState(
        level = level,
        voltageMv = voltageMv,
        temperatureC = temperatureC,
        currentMa = MeasuredValue(currentMa ?: 0, confidence),
        chargingStatus = enumValueOrDefault(status, ChargingStatus.NOT_CHARGING),
        plugType = enumValueOrDefault(plugType, PlugType.NONE),
        health = enumValueOrDefault(health, BatteryHealth.UNKNOWN),
        technology = "",
        cycleCount = cycleCount,
        healthPercent = healthPct
    )
}

private fun NetworkReadingEntity.toNetworkState(): NetworkState {
    val connectionType = enumValueOrDefault(type, ConnectionType.NONE)
    return NetworkState(
        connectionType = connectionType,
        signalDbm = signalDbm,
        signalQuality = classifySignal(signalDbm, connectionType),
        wifiSpeedMbps = wifiSpeedMbps,
        wifiFrequencyMhz = wifiFrequency,
        carrier = carrier,
        networkSubtype = networkSubtype,
        latencyMs = latencyMs
    )
}

private fun ThermalReadingEntity.toThermalState(): ThermalState =
    ThermalState(
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = ThermalStatus.entries.getOrElse(thermalStatus) { ThermalStatus.NONE },
        isThrottling = throttling
    )

private fun StorageReadingEntity.toStorageState(): StorageState {
    val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usagePercent = if (totalBytes > 0) {
        (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
    } else {
        0f
    }

    return StorageState(
        totalBytes = totalBytes,
        availableBytes = availableBytes,
        usedBytes = usedBytes,
        usagePercent = usagePercent,
        appsBytes = appsBytes
    )
}

private fun classifySignal(dbm: Int?, type: ConnectionType): SignalQuality {
    if (type == ConnectionType.NONE) return SignalQuality.NO_SIGNAL
    if (type == ConnectionType.VPN && dbm == null) return SignalQuality.GOOD
    if (dbm == null) return SignalQuality.NO_SIGNAL

    return when (type) {
        ConnectionType.WIFI -> when {
            dbm > -50 -> SignalQuality.EXCELLENT
            dbm > -60 -> SignalQuality.GOOD
            dbm > -70 -> SignalQuality.FAIR
            dbm > -80 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }

        ConnectionType.CELLULAR -> when {
            dbm > -80 -> SignalQuality.EXCELLENT
            dbm > -90 -> SignalQuality.GOOD
            dbm > -100 -> SignalQuality.FAIR
            dbm > -110 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }

        ConnectionType.VPN -> when {
            dbm > -80 -> SignalQuality.EXCELLENT
            dbm > -90 -> SignalQuality.GOOD
            dbm > -100 -> SignalQuality.FAIR
            dbm > -110 -> SignalQuality.POOR
            else -> SignalQuality.NO_SIGNAL
        }

        ConnectionType.NONE -> SignalQuality.NO_SIGNAL
    }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)
