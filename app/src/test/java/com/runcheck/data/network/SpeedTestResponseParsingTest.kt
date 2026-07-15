package com.runcheck.data.network

import net.measurementlab.ndt7.android.NdtTest.TestType
import net.measurementlab.ndt7.android.models.AppInfo
import net.measurementlab.ndt7.android.models.ClientResponse
import net.measurementlab.ndt7.android.utils.DataConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedTestResponseParsingTest {
    @Test
    fun `origin and test map to server name and location`() {
        val response = ClientResponse(AppInfo(2_000_000L, 12_500_000.0), "mlab1.example.net", "Helsinki")

        assertEquals(
            ServerMetadata(name = "mlab1.example.net", location = "Helsinki"),
            parseServerMetadata(response),
        )
    }

    @Test
    fun `missing or protocol-only values do not fabricate server metadata`() {
        assertNull(parseServerMetadata(null))
        assertNull(parseServerMetadata(ClientResponse(AppInfo(1L, 1.0), "", "")))
        assertNull(DataConverter.generateResponse(0L, 1.0, TestType.DOWNLOAD).let(::parseServerMetadata))
    }

    @Test
    fun `ndt7 app info converts bytes and microseconds to megabits per second`() {
        val response = ClientResponse(AppInfo(2_000_000L, 12_500_000.0), null, null)

        assertEquals(50.0, DataConverter.convertToMbps(response), 0.000_001)
    }
}
