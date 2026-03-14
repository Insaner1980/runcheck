package com.runcheck.domain.usecase

import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.ThermalStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository,
    private val fileExportRepository: FileExportRepository,
    private val proStatusProvider: ProStatusProvider,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

    private fun formatTimestamp(epochMs: Long): String =
        isoFormatter.format(Instant.ofEpochMilli(epochMs))

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        return if (
            value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        ) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun requirePro() {
        check(proStatusProvider.isPro()) { "CSV export requires Runcheck Pro" }
    }

    private suspend fun exportCutoff(): Long? {
        val retention = userPreferencesRepository.getPreferences().first().dataRetention
        val now = System.currentTimeMillis()
        return retention.durationMillis?.let { now - it }
    }

    private fun <T> List<T>.filterByRetention(
        cutoff: Long?,
        timestampOf: (T) -> Long
    ): List<T> = if (cutoff == null) this else filter { timestampOf(it) >= cutoff }

    private fun formatThermalStatus(status: Int): String =
        ThermalStatus.entries.getOrNull(status)?.name ?: status.toString()

    suspend fun exportBatteryCsv(): String {
        requirePro()
        return withContext(Dispatchers.IO) {
            val cutoff = exportCutoff()
            val readings = batteryRepository.getAllReadings()
                .filterByRetention(cutoff) { it.timestamp }
            buildString {
                appendLine("timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence,status,plug_type,health,cycle_count,health_pct")
                for (r in readings) {
                    appendLine(
                        "${formatTimestamp(r.timestamp)},${r.level},${r.voltageMv},${r.temperatureC}," +
                            "${r.currentMa ?: ""},${escapeCsv(r.currentConfidence)},${escapeCsv(r.status)}," +
                            "${escapeCsv(r.plugType)},${escapeCsv(r.health)},${r.cycleCount ?: ""},${r.healthPct ?: ""}"
                    )
                }
            }
        }
    }

    suspend fun exportNetworkCsv(): String {
        requirePro()
        return withContext(Dispatchers.IO) {
            val cutoff = exportCutoff()
            val readings = networkRepository.getAllReadings()
                .filterByRetention(cutoff) { it.timestamp }
            buildString {
                appendLine("timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms")
                for (r in readings) {
                    appendLine(
                        "${formatTimestamp(r.timestamp)},${escapeCsv(r.type)},${r.signalDbm ?: ""}," +
                            "${r.wifiSpeedMbps ?: ""},${r.wifiFrequency ?: ""},${escapeCsv(r.carrier)}," +
                            "${escapeCsv(r.networkSubtype)},${r.latencyMs ?: ""}"
                    )
                }
            }
        }
    }

    suspend fun exportThermalCsv(): String {
        requirePro()
        return withContext(Dispatchers.IO) {
            val cutoff = exportCutoff()
            val readings = thermalRepository.getAllReadings()
                .filterByRetention(cutoff) { it.timestamp }
            buildString {
                appendLine("timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling")
                for (r in readings) {
                    appendLine(
                        "${formatTimestamp(r.timestamp)},${r.batteryTempC},${r.cpuTempC ?: ""}," +
                            "${escapeCsv(formatThermalStatus(r.thermalStatus))},${r.throttling}"
                    )
                }
            }
        }
    }

    suspend fun exportStorageCsv(): String {
        requirePro()
        return withContext(Dispatchers.IO) {
            val cutoff = exportCutoff()
            val readings = storageRepository.getAllReadings()
                .filterByRetention(cutoff) { it.timestamp }
            buildString {
                appendLine("timestamp,total_bytes,available_bytes,apps_bytes,media_bytes")
                for (r in readings) {
                    appendLine(
                        "${formatTimestamp(r.timestamp)},${r.totalBytes},${r.availableBytes}," +
                            "${r.appsBytes},${r.mediaBytes}"
                    )
                }
            }
        }
    }

    suspend fun exportAllCsv(): Map<String, String> = mapOf(
        "runcheck_battery.csv" to exportBatteryCsv(),
        "runcheck_network.csv" to exportNetworkCsv(),
        "runcheck_thermal.csv" to exportThermalCsv(),
        "runcheck_storage.csv" to exportStorageCsv()
    )

    suspend fun prepareExportShare() =
        fileExportRepository.prepareExportShare(exportAllCsv())
}
