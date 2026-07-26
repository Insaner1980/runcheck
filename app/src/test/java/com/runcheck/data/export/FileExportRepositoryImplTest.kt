package com.runcheck.data.export

import org.junit.Assert.assertThrows
import org.junit.Test

class FileExportRepositoryImplTest {
    @Test
    fun `safe CSV filenames are accepted`() {
        requireSafeExportFileName("battery_readings-2026.csv")
    }

    @Test
    fun `path traversal and non CSV filenames are rejected`() {
        listOf(
            "../private.csv",
            "..\\private.csv",
            "nested/readings.csv",
            "nested\\readings.csv",
            ".csv",
            "readings.txt",
            "",
        ).forEach { fileName ->
            assertThrows(IllegalArgumentException::class.java) {
                requireSafeExportFileName(fileName)
            }
        }
    }
}
