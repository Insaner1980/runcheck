package com.runcheck.domain.usecase

import com.runcheck.domain.model.BatteryReading
import com.runcheck.domain.model.DataRetention
import com.runcheck.domain.model.NetworkReading
import com.runcheck.domain.model.StorageReading
import com.runcheck.domain.model.ThermalReading
import com.runcheck.domain.model.UserPreferences
import com.runcheck.domain.repository.BatteryRepository
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.domain.repository.NetworkRepository
import com.runcheck.domain.repository.ProStatusProvider
import com.runcheck.domain.repository.StorageRepository
import com.runcheck.domain.repository.ThermalRepository
import com.runcheck.domain.repository.UserPreferencesRepository
import com.runcheck.util.AppDispatchers
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import kotlin.coroutines.CoroutineContext

class ExportDataUseCaseTest {
    @Test
    fun `thermal CSV preserves canonical identifiers and unknown raw codes`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { thermalRepository.getAllReadings() } returns
                    listOf(
                        ThermalReading(1_700_000_000_000L, 30f, null, 0, false),
                        ThermalReading(1_700_000_001_000L, 31f, 51f, 1, false),
                        ThermalReading(1_700_000_002_000L, 32f, 52f, 2, false),
                        ThermalReading(1_700_000_003_000L, 33f, null, 3, true),
                        ThermalReading(1_700_000_004_000L, 34f, 54f, 4, true),
                        ThermalReading(1_700_000_005_000L, 35f, 55f, 5, true),
                        ThermalReading(1_700_000_006_000L, 36f, 56f, 6, true),
                        ThermalReading(1_700_000_007_000L, 37f, null, 99, false),
                    )

                assertEquals(
                    "timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling\n" +
                        "2023-11-14T22:13:20Z,30.0,,NONE,false\n" +
                        "2023-11-14T22:13:21Z,31.0,51.0,LIGHT,false\n" +
                        "2023-11-14T22:13:22Z,32.0,52.0,MODERATE,false\n" +
                        "2023-11-14T22:13:23Z,33.0,,SEVERE,true\n" +
                        "2023-11-14T22:13:24Z,34.0,54.0,CRITICAL,true\n" +
                        "2023-11-14T22:13:25Z,35.0,55.0,EMERGENCY,true\n" +
                        "2023-11-14T22:13:26Z,36.0,56.0,SHUTDOWN,true\n" +
                        "2023-11-14T22:13:27Z,37.0,,99,false\n",
                    useCase.exportThermalCsv(),
                )
            }
        }

    private lateinit var useCase: ExportDataUseCase
    private lateinit var batteryRepository: BatteryRepository
    private lateinit var networkRepository: NetworkRepository
    private lateinit var thermalRepository: ThermalRepository
    private lateinit var storageRepository: StorageRepository
    private lateinit var fileExportRepository: FileExportRepository
    private lateinit var proStatusProvider: ProStatusProvider
    private lateinit var userPreferencesRepository: UserPreferencesRepository

    @Before
    fun setup() {
        batteryRepository = mockk()
        networkRepository = mockk()
        thermalRepository = mockk()
        storageRepository = mockk()
        fileExportRepository = mockk()
        proStatusProvider = mockk()
        userPreferencesRepository = mockk()

        every { proStatusProvider.isPro() } returns true
        every { userPreferencesRepository.getPreferences() } returns
            flowOf(
                UserPreferences(dataRetention = DataRetention.FOREVER),
            )

        // Default empty data
        coEvery { batteryRepository.getAllReadings() } returns emptyList()
        coEvery { networkRepository.getAllReadings() } returns emptyList()
        coEvery { thermalRepository.getAllReadings() } returns emptyList()
        coEvery { storageRepository.getAllReadings() } returns emptyList()

        useCase = createUseCase()
    }

    private fun createUseCase(dispatchers: AppDispatchers = AppDispatchers()) =
        ExportDataUseCase(
            batteryRepository = batteryRepository,
            networkRepository = networkRepository,
            thermalRepository = thermalRepository,
            storageRepository = storageRepository,
            fileExportRepository = fileExportRepository,
            proStatusProvider = proStatusProvider,
            userPreferencesRepository = userPreferencesRepository,
            dispatchers = dispatchers,
        )

    private suspend fun withUtcDefaultTimeZone(block: suspend () -> Unit) {
        val originalTimeZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            useCase = createUseCase()
            block()
        } finally {
            TimeZone.setDefault(originalTimeZone)
        }
    }

    // --- CSV escaping tests (via battery export) ---

    @Test
    fun `CSV escaping - value with comma is quoted`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    batteryReading(
                        voltageMv = 4000,
                        temperatureC = 30f,
                        currentMa = -400,
                        currentConfidence = "HIGH,MEDIUM", // contains comma
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // "HIGH,MEDIUM" should be wrapped in quotes
            assertTrue(
                "Value with comma should be quoted: $dataLine",
                dataLine.contains("\"HIGH,MEDIUM\""),
            )
        }

    @Test
    fun `CSV escaping - value with quote is double-quoted`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    batteryReading(
                        voltageMv = 4000,
                        temperatureC = 30f,
                        currentMa = -400,
                        currentConfidence = "says \"hello\"", // contains quotes
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // Quotes inside should be doubled and whole value quoted
            assertTrue(
                "Value with quotes should be escaped: $dataLine",
                dataLine.contains("\"says \"\"hello\"\"\""),
            )
        }

    @Test
    fun `CSV escaping - value with newline is quoted`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns
                listOf(
                    NetworkReading(
                        timestamp = 1_700_000_000_000L,
                        type = "WIFI",
                        signalDbm = -50,
                        wifiSpeedMbps = 100,
                        wifiFrequency = 5000,
                        carrier = "Test\nCarrier", // contains newline
                        networkSubtype = null,
                        latencyMs = 20,
                    ),
                )

            val csv = useCase.exportNetworkCsv()

            // The carrier value should be wrapped in quotes
            assertTrue(
                "Value with newline should be quoted",
                csv.contains("\"Test\nCarrier\""),
            )
        }

    // --- Empty data tests ---

    @Test
    fun `empty battery data produces header row only`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns emptyList()

            assertEquals(
                "timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence," +
                    "status,plug_type,health,cycle_count,health_pct\n",
                useCase.exportBatteryCsv(),
            )
        }

    @Test
    fun `empty network data produces header row only`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns emptyList()

            assertEquals(
                "timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms\n",
                useCase.exportNetworkCsv(),
            )
        }

    @Test
    fun `empty thermal data produces header row only`() =
        runTest {
            coEvery { thermalRepository.getAllReadings() } returns emptyList()

            assertEquals(
                "timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling\n",
                useCase.exportThermalCsv(),
            )
        }

    @Test
    fun `empty storage data produces header row only`() =
        runTest {
            coEvery { storageRepository.getAllReadings() } returns emptyList()

            assertEquals(
                "timestamp,total_bytes,available_bytes,apps_bytes,media_bytes\n",
                useCase.exportStorageCsv(),
            )
        }

    // --- Normal export tests ---

    @Test
    fun `battery export has correct column count`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(batteryReading())

            val csv = useCase.exportBatteryCsv()
            val lines = csv.lines().filter { it.isNotBlank() }

            assertEquals(2, lines.size) // header + 1 data row

            // Header has 11 columns
            val headerCols = lines[0].split(",")
            assertEquals(11, headerCols.size)

            // Data row should also have 11 columns
            val dataCols = lines[1].split(",")
            assertEquals(11, dataCols.size)
        }

    @Test
    fun `battery export preserves exact columns values row order and trailing newline`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { batteryRepository.getAllReadings() } returns
                    listOf(
                        BatteryReading(
                            timestamp = 1_700_000_000_123L,
                            level = 81,
                            voltageMv = 4217,
                            temperatureC = 32.5f,
                            currentMa = -463,
                            currentConfidence = "HIGH",
                            status = "CHARGING",
                            plugType = "USB",
                            health = "GOOD",
                            cycleCount = 157,
                            healthPct = 96,
                        ),
                        batteryReading(),
                    )

                assertEquals(
                    "timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence," +
                        "status,plug_type,health,cycle_count,health_pct\n" +
                        "2023-11-14T22:13:20.123Z,81,4217,32.5,-463,HIGH,CHARGING,USB,GOOD,157,96\n" +
                        "2023-11-14T22:13:20Z,80,4200,32.5,-450,HIGH,DISCHARGING,NONE,GOOD,150,95\n",
                    useCase.exportBatteryCsv(),
                )
            }
        }

    @Test
    fun `battery export handles null optional fields as empty`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { batteryRepository.getAllReadings() } returns
                    listOf(
                        BatteryReading(
                            timestamp = 1_700_000_000_123L,
                            level = 81,
                            voltageMv = 4217,
                            temperatureC = 32.5f,
                            currentMa = null,
                            currentConfidence = "UNAVAILABLE",
                            status = "DISCHARGING",
                            plugType = "NONE",
                            health = "UNKNOWN",
                            cycleCount = null,
                            healthPct = null,
                        ),
                    )

                assertEquals(
                    "timestamp,level,voltage_mv,temperature_c,current_ma,current_confidence," +
                        "status,plug_type,health,cycle_count,health_pct\n" +
                        "2023-11-14T22:13:20.123Z,81,4217,32.5,,UNAVAILABLE,DISCHARGING,NONE,UNKNOWN,,\n",
                    useCase.exportBatteryCsv(),
                )
            }
        }

    @Test
    fun `network export preserves exact columns values escaping and trailing newline`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { networkRepository.getAllReadings() } returns
                    listOf(
                        NetworkReading(
                            timestamp = 1_700_000_000_123L,
                            type = "WIFI",
                            signalDbm = -53,
                            wifiSpeedMbps = 173,
                            wifiFrequency = 5180,
                            carrier = "Nordic, \"5G\" Ω",
                            networkSubtype = "LTE-A",
                            latencyMs = 29,
                        ),
                    )

                assertEquals(
                    "timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms\n" +
                        "2023-11-14T22:13:20.123Z,WIFI,-53,173,5180,\"Nordic, \"\"5G\"\" Ω\",LTE-A,29\n",
                    useCase.exportNetworkCsv(),
                )
            }
        }

    @Test
    fun `network export preserves unknown raw connection type`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { networkRepository.getAllReadings() } returns
                    listOf(
                        NetworkReading(
                            timestamp = 1_700_000_000_123L,
                            type = "SATELLITE",
                            signalDbm = -45,
                            wifiSpeedMbps = null,
                            wifiFrequency = null,
                            carrier = null,
                            networkSubtype = null,
                            latencyMs = 20,
                        ),
                    )

                assertEquals(
                    "timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms\n" +
                        "2023-11-14T22:13:20.123Z,SATELLITE,-45,,,,,20\n",
                    useCase.exportNetworkCsv(),
                )
            }
        }

    @Test
    fun `thermal export has correct header`() =
        runTest {
            val csv = useCase.exportThermalCsv()
            val header = csv.lines().first()

            assertEquals(
                "timestamp,battery_temp_c,cpu_temp_c,thermal_status,throttling",
                header,
            )
        }

    @Test
    fun `storage export has correct header`() =
        runTest {
            val csv = useCase.exportStorageCsv()
            val header = csv.lines().first()

            assertEquals(
                "timestamp,total_bytes,available_bytes,apps_bytes,media_bytes",
                header,
            )
        }

    // --- exportAllCsv tests ---

    @Test
    fun `exportAllCsv returns 4 entries`() =
        runTest {
            val result = useCase.exportAllCsv()

            assertEquals(4, result.size)
            assertTrue(result.containsKey("runcheck_battery.csv"))
            assertTrue(result.containsKey("runcheck_network.csv"))
            assertTrue(result.containsKey("runcheck_thermal.csv"))
            assertTrue(result.containsKey("runcheck_storage.csv"))
        }

    @Test
    fun `exportAllCsv values are non-empty strings`() =
        runTest {
            val result = useCase.exportAllCsv()

            result.values.forEach { csv ->
                assertTrue("Each CSV should have at least a header", csv.isNotBlank())
            }
        }

    @Test
    fun `prepareExportShare builds csv payloads on default dispatcher before writing files`() =
        runTest {
            val recordingDispatcher = RecordingDispatcher()
            val useCase = createUseCase(TestAppDispatchers(defaultDispatcher = recordingDispatcher))
            coEvery { fileExportRepository.prepareExportShare(any()) } returns listOf("content://runcheck/export.zip")

            val result = useCase.prepareExportShare()

            assertEquals(listOf("content://runcheck/export.zip"), result)
            assertTrue(
                "CSV generation should switch to the injected default dispatcher",
                recordingDispatcher.dispatchCount > 0,
            )
            coVerify(exactly = 1) {
                fileExportRepository.prepareExportShare(
                    match { files ->
                        files.keys ==
                            setOf(
                                "runcheck_battery.csv",
                                "runcheck_network.csv",
                                "runcheck_thermal.csv",
                                "runcheck_storage.csv",
                            )
                    },
                )
            }
        }

    // --- Pro gate tests ---

    @Test
    fun `share preparation stops if pro is revoked while reading the final dataset`() =
        runTest {
            var isPro = true
            every { proStatusProvider.isPro() } answers { isPro }
            coEvery { storageRepository.getAllReadings() } coAnswers {
                isPro = false
                emptyList()
            }
            coEvery { fileExportRepository.prepareExportShare(any()) } returns listOf("content://runcheck/export.csv")

            val result = runCatching { useCase.prepareExportShare() }

            assertTrue(result.exceptionOrNull() is IllegalStateException)
            coVerify(exactly = 0) { fileExportRepository.prepareExportShare(any()) }
        }

    @Test
    fun `share preparation clears cached exports if pro is revoked while writing files`() =
        runTest {
            var isPro = true
            every { proStatusProvider.isPro() } answers { isPro }
            coEvery { fileExportRepository.prepareExportShare(any()) } coAnswers {
                isPro = false
                listOf("content://runcheck/export.csv")
            }
            coEvery { fileExportRepository.clearPreparedExports() } returns Unit

            val result = runCatching { useCase.prepareExportShare() }

            assertTrue(result.exceptionOrNull() is IllegalStateException)
            coVerify(exactly = 1) { fileExportRepository.clearPreparedExports() }
        }

    @Test(expected = IllegalStateException::class)
    fun `battery export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportBatteryCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `network export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportNetworkCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `thermal export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportThermalCsv()
        }

    @Test(expected = IllegalStateException::class)
    fun `storage export throws for non-Pro user`() =
        runTest {
            every { proStatusProvider.isPro() } returns false

            useCase.exportStorageCsv()
        }

    // --- Timestamp formatting ---

    @Test
    fun `timestamps are formatted as ISO 8601`() =
        runTest {
            coEvery { batteryRepository.getAllReadings() } returns
                listOf(
                    batteryReading(
                        temperatureC = 30f,
                        currentMa = null,
                        cycleCount = null,
                        healthPct = null,
                    ),
                )

            val csv = useCase.exportBatteryCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            // ISO 8601 timestamp should contain 'T' and timezone offset (+ or Z)
            val timestampField = dataLine.split(",").first()
            assertTrue(
                "Timestamp should be ISO 8601 format: $timestampField",
                timestampField.contains("T"),
            )
        }

    private fun batteryReading(
        voltageMv: Int = 4200,
        temperatureC: Float = 32.5f,
        currentMa: Int? = -450,
        currentConfidence: String = "HIGH",
        cycleCount: Int? = 150,
        healthPct: Int? = 95,
    ) = BatteryReading(
        timestamp = 1_700_000_000_000L,
        level = 80,
        voltageMv = voltageMv,
        temperatureC = temperatureC,
        currentMa = currentMa,
        currentConfidence = currentConfidence,
        status = "DISCHARGING",
        plugType = "NONE",
        health = "GOOD",
        cycleCount = cycleCount,
        healthPct = healthPct,
    )

    // --- Thermal status formatting ---

    @Test
    fun `thermal status integer is formatted as enum name`() =
        runTest {
            coEvery { thermalRepository.getAllReadings() } returns
                listOf(
                    ThermalReading(
                        timestamp = 1_700_000_000_000L,
                        batteryTempC = 35f,
                        cpuTempC = 60f,
                        thermalStatus = 3, // SEVERE (0=NONE, 1=LIGHT, 2=MODERATE, 3=SEVERE)
                        throttling = true,
                    ),
                )

            val csv = useCase.exportThermalCsv()
            val dataLine = csv.lines().drop(1).first { it.isNotBlank() }

            assertTrue(
                "Thermal status 3 should be formatted as SEVERE: $dataLine",
                dataLine.contains("SEVERE"),
            )
        }

    @Test
    fun `storage export preserves exact positive byte values`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { storageRepository.getAllReadings() } returns
                    listOf(
                        StorageReading(
                            timestamp = 1_700_000_000_123L,
                            totalBytes = 128_000_000_000L,
                            availableBytes = 64_000_000_000L,
                            appsBytes = 20_000_000_000L,
                            mediaBytes = 30_000_000_000L,
                        ),
                    )

                assertEquals(
                    "timestamp,total_bytes,available_bytes,apps_bytes,media_bytes\n" +
                        "2023-11-14T22:13:20.123Z,128000000000,64000000000,20000000000,30000000000\n",
                    useCase.exportStorageCsv(),
                )
            }
        }

    @Test
    fun `storage export distinguishes positive zero and unavailable media bytes`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { storageRepository.getAllReadings() } returns
                    listOf(
                        StorageReading(1_700_000_000_123L, 128L, 64L, 20L, 30L),
                        StorageReading(1_700_000_001_456L, 256L, 96L, null, 0L),
                        StorageReading(1_700_000_002_789L, 512L, 128L, null, null),
                    )

                assertEquals(
                    "timestamp,total_bytes,available_bytes,apps_bytes,media_bytes\n" +
                        "2023-11-14T22:13:20.123Z,128,64,20,30\n" +
                        "2023-11-14T22:13:21.456Z,256,96,,0\n" +
                        "2023-11-14T22:13:22.789Z,512,128,,\n",
                    useCase.exportStorageCsv(),
                )
            }
        }

    @Test
    fun `storage export leaves unavailable app bytes empty instead of writing zero`() =
        runTest {
            coEvery { storageRepository.getAllReadings() } returns
                listOf(
                    StorageReading(
                        timestamp = 1_700_000_000_000L,
                        totalBytes = 128_000_000_000L,
                        availableBytes = 64_000_000_000L,
                        appsBytes = null,
                        mediaBytes = 30_000_000_000L,
                    ),
                )

            val dataLine =
                useCase
                    .exportStorageCsv()
                    .lines()
                    .drop(1)
                    .first { it.isNotBlank() }

            assertEquals(5, dataLine.split(',').size)
            assertTrue(
                "Unavailable apps bytes should be an empty CSV field: $dataLine",
                dataLine.contains(",,30000000000"),
            )
        }

    // --- CSV escaping edge case: carriage return ---

    @Test
    fun `CSV escaping - value with carriage return is quoted`() =
        runTest {
            coEvery { networkRepository.getAllReadings() } returns
                listOf(
                    NetworkReading(
                        timestamp = 1_700_000_000_000L,
                        type = "WIFI",
                        signalDbm = -50,
                        wifiSpeedMbps = null,
                        wifiFrequency = null,
                        carrier = "Test\rCarrier", // contains carriage return
                        networkSubtype = null,
                        latencyMs = null,
                    ),
                )

            val csv = useCase.exportNetworkCsv()

            assertTrue(
                "Value with carriage return should be quoted",
                csv.contains("\"Test\rCarrier\""),
            )
        }

    @Test
    fun `null values in CSV are empty strings`() =
        runTest {
            withUtcDefaultTimeZone {
                coEvery { networkRepository.getAllReadings() } returns
                    listOf(
                        NetworkReading(
                            timestamp = 1_700_000_000_123L,
                            type = "CELLULAR",
                            signalDbm = null,
                            wifiSpeedMbps = null,
                            wifiFrequency = null,
                            carrier = null,
                            networkSubtype = null,
                            latencyMs = null,
                        ),
                    )

                assertEquals(
                    "timestamp,type,signal_dbm,wifi_speed_mbps,wifi_frequency,carrier,network_subtype,latency_ms\n" +
                        "2023-11-14T22:13:20.123Z,CELLULAR,,,,,,\n",
                    useCase.exportNetworkCsv(),
                )
            }
        }

    private class TestAppDispatchers(
        private val defaultDispatcher: CoroutineDispatcher,
    ) : AppDispatchers() {
        override val default: CoroutineDispatcher
            get() = defaultDispatcher
    }

    private class RecordingDispatcher : CoroutineDispatcher() {
        var dispatchCount: Int = 0
            private set

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatchCount += 1
            block.run()
        }
    }
}
