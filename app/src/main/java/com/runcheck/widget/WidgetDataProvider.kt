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
import com.runcheck.domain.model.MonitoringFreshnessPolicy
import com.runcheck.domain.model.MonitoringInterval
import com.runcheck.domain.model.NetworkState
import com.runcheck.domain.model.PlugType
import com.runcheck.domain.model.StorageState
import com.runcheck.domain.model.ThermalState
import com.runcheck.domain.model.ThermalStatus
import com.runcheck.domain.model.classifyNetworkSignalQuality
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.scoring.HealthScoreCalculator
import com.runcheck.util.enumValueOrDefault
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal data class BatteryWidgetSnapshot(
    val level: Int,
    val temperatureC: Float,
    val currentMa: Int?,
)

internal data class HealthWidgetSnapshot(
    val overallScore: Int,
    val batteryLevel: Int,
)

internal data class HealthWidgetReadings(
    val battery: BatteryReadingEntity?,
    val network: NetworkReadingEntity?,
    val thermal: ThermalReadingEntity?,
    val storage: StorageReadingEntity?,
)

internal sealed interface WidgetRenderState<out T> {
    data object Locked : WidgetRenderState<Nothing>

    data object Empty : WidgetRenderState<Nothing>

    data object Stale : WidgetRenderState<Nothing>

    data class Content<T>(
        val snapshot: T,
    ) : WidgetRenderState<T>
}

internal object WidgetDataProvider {
    fun observeBatteryWidgetState(context: Context): Flow<WidgetRenderState<BatteryWidgetSnapshot>> {
        val ep = entryPoint(context)
        return combine(
            ep.proStatusProvider().isProUser,
            ep.batteryReadingDao().getLatestReading(),
        ) { isPro, latestReading ->
            when {
                !isPro -> WidgetRenderState.Locked
                latestReading == null -> WidgetRenderState.Empty
                else -> WidgetRenderState.Content(latestReading.toBatteryWidgetSnapshot())
            }
        }
    }

    fun observeHealthWidgetState(context: Context): Flow<WidgetRenderState<HealthWidgetSnapshot>> {
        val ep = entryPoint(context)
        val accessFlow =
            combine(
                ep.proStatusProvider().isProUser,
                ep.userPreferencesRepository().getPreferences(),
            ) { isPro, preferences -> isPro to preferences.monitoringInterval }
        return combine(
            accessFlow,
            ep.batteryReadingDao().getLatestReading(),
            ep.networkReadingDao().getLatestReading(),
            ep.thermalReadingDao().getLatestReading(),
            ep.storageReadingDao().getLatestReading(),
        ) { access, batteryReading, networkReading, thermalReading, storageReading ->
            healthWidgetRenderState(
                isPro = access.first,
                monitoringInterval = access.second,
                readings =
                    HealthWidgetReadings(
                        battery = batteryReading,
                        network = networkReading,
                        thermal = thermalReading,
                        storage = storageReading,
                    ),
                nowMillis = System.currentTimeMillis(),
                calculator = ep.healthScoreCalculator(),
            )
        }
    }

    private fun entryPoint(context: Context): WidgetDataEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetDataEntryPoint::class.java,
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

    fun userPreferencesRepository(): UserPreferencesRepository
}

internal fun healthWidgetRenderState(
    isPro: Boolean,
    monitoringInterval: MonitoringInterval,
    readings: HealthWidgetReadings,
    nowMillis: Long,
    calculator: HealthScoreCalculator,
): WidgetRenderState<HealthWidgetSnapshot> {
    if (!isPro) return WidgetRenderState.Locked
    val batteryReading = readings.battery
    val networkReading = readings.network
    val thermalReading = readings.thermal
    val storageReading = readings.storage
    if (batteryReading == null || networkReading == null || thermalReading == null || storageReading == null) {
        return WidgetRenderState.Empty
    }

    val timestamps =
        listOf(
            batteryReading.timestamp,
            networkReading.timestamp,
            thermalReading.timestamp,
            storageReading.timestamp,
        )
    val oldestTimestamp = timestamps.min()
    val newestTimestamp = timestamps.max()
    val staleThresholdMillis = MonitoringFreshnessPolicy.staleAfterMillis(monitoringInterval.minutes)
    val isInvalidOrStale =
        oldestTimestamp < 0L ||
            newestTimestamp > nowMillis ||
            newestTimestamp - oldestTimestamp > HEALTH_INPUT_WINDOW_MS ||
            nowMillis - oldestTimestamp > staleThresholdMillis
    if (isInvalidOrStale) return WidgetRenderState.Stale

    val battery = batteryReading.toBatteryState()
    val score =
        calculator.calculate(
            battery = battery,
            network = networkReading.toNetworkState(),
            thermal = thermalReading.toThermalState(),
            storage = storageReading.toStorageState(),
        )

    return WidgetRenderState.Content(
        HealthWidgetSnapshot(
            overallScore = score.overallScore,
            batteryLevel = battery.level,
        ),
    )
}

private const val HEALTH_INPUT_WINDOW_MS = 120_000L

internal fun BatteryReadingEntity.toBatteryWidgetSnapshot(): BatteryWidgetSnapshot =
    BatteryWidgetSnapshot(
        level = level,
        temperatureC = temperatureC,
        currentMa = currentMa.takeIf { parsedCurrentConfidence() != Confidence.UNAVAILABLE },
    )

private fun BatteryReadingEntity.toBatteryState(): BatteryState {
    val confidence = parsedCurrentConfidence()

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
        healthPercent = healthPct,
    )
}

private fun BatteryReadingEntity.parsedCurrentConfidence(): Confidence =
    runCatching { Confidence.valueOf(currentConfidence) }
        .getOrDefault(Confidence.UNAVAILABLE)

private fun NetworkReadingEntity.toNetworkState(): NetworkState {
    val connectionType = enumValueOrDefault(type, ConnectionType.NONE)
    return NetworkState(
        connectionType = connectionType,
        signalDbm = signalDbm,
        signalQuality = classifyNetworkSignalQuality(signalDbm, connectionType, networkSubtype),
        wifiSpeedMbps = wifiSpeedMbps,
        wifiFrequencyMhz = wifiFrequency,
        carrier = carrier,
        networkSubtype = networkSubtype,
        latencyMs = latencyMs,
    )
}

private fun ThermalReadingEntity.toThermalState(): ThermalState =
    ThermalState(
        batteryTempC = batteryTempC,
        cpuTempC = cpuTempC,
        thermalStatus = ThermalStatus.entries.getOrElse(thermalStatus) { ThermalStatus.NONE },
        isThrottling = throttling,
    )

private fun StorageReadingEntity.toStorageState(): StorageState {
    val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usagePercent =
        if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes.toFloat()) * 100f
        } else {
            0f
        }

    return StorageState(
        totalBytes = totalBytes,
        availableBytes = availableBytes,
        usedBytes = usedBytes,
        usagePercent = usagePercent,
        appsBytes = appsBytes,
    )
}
