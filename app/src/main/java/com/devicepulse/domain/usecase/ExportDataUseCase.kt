package com.devicepulse.domain.usecase

import com.devicepulse.domain.repository.BatteryRepository
import com.devicepulse.domain.repository.NetworkRepository
import com.devicepulse.domain.repository.StorageRepository
import com.devicepulse.domain.repository.ThermalRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ExportDataUseCase @Inject constructor(
    private val batteryRepository: BatteryRepository,
    private val networkRepository: NetworkRepository,
    private val thermalRepository: ThermalRepository,
    private val storageRepository: StorageRepository
) {

    private val isoFormatter: DateTimeFormatter =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault())

    private fun formatTimestamp(epochMs: Long): String =
        isoFormatter.format(Instant.ofEpochMilli(epochMs))

    private fun escapeCsv(value: String?): String {
        if (value == null) return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    suspend fun exportBatteryCsv(): String {
        val readings = batteryRepository.getAllReadings()
        val sb = StringBuilder()
        sb.appendLine("timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence,status,plug_type,health,cycle_count,health_pct")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.level},${r.voltageMv},${r.temperatureC}," +
                    "${r.currentMa ?: ""},${escapeCsv(r.currentConfidence)},${escapeCsv(r.status)}," +
                    "${escapeCsv(r.plugType)},${escapeCsv(r.health)},${r.cycleCount ?: ""},${r.healthPct ?: ""}"
            )
        }
        return sb.toString()
    }

    suspend fun exportNetworkCsv(): String {
        val readings = networkRepository.getAllReadings()
        val sb = StringBuilder()
        sb.appendLine("timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${escapeCsv(r.type)},${r.signalDbm ?: ""}," +
                    "${r.wifiSpeedMbps ?: ""},${r.wifiFrequency ?: ""},${escapeCsv(r.carrier)}," +
                    "${escapeCsv(r.networkSubtype)},${r.latencyMs ?: ""}"
            )
        }
        return sb.toString()
    }

    suspend fun exportThermalCsv(): String {
        val readings = thermalRepository.getAllReadings()
        val sb = StringBuilder()
        sb.appendLine("timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.batteryTempC},${r.cpuTempC ?: ""}," +
                    "${r.thermalStatus},${r.throttling}"
            )
        }
        return sb.toString()
    }

    suspend fun exportStorageCsv(): String {
        val readings = storageRepository.getAllReadings()
        val sb = StringBuilder()
        sb.appendLine("timestamp,total_bytes,available_bytes,apps_bytes,media_bytes")
        for (r in readings) {
            sb.appendLine(
                "${formatTimestamp(r.timestamp)},${r.totalBytes},${r.availableBytes}," +
                    "${r.appsBytes},${r.mediaBytes}"
            )
        }
        return sb.toString()
    }

    suspend fun exportAllCsv(): Map<String, String> = mapOf(
        "devicepulse_battery.csv" to exportBatteryCsv(),
        "devicepulse_network.csv" to exportNetworkCsv(),
        "devicepulse_thermal.csv" to exportThermalCsv(),
        "devicepulse_storage.csv" to exportStorageCsv()
    )
}
