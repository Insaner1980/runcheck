package com.runcheck.data.export

import android.content.Context
import com.runcheck.util.TestAppDispatchers
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class FileExportRepositoryImplTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var repository: FileExportRepositoryImpl
    private lateinit var exportRoot: File

    @Before
    fun setUp() {
        val context = mockk<Context>()
        every { context.cacheDir } returns temporaryFolder.root
        repository = FileExportRepositoryImpl(context, TestAppDispatchers())
        exportRoot = File(temporaryFolder.root, "exports")
    }

    @Test
    fun `clearing missing prepared exports succeeds`() =
        runTest {
            repository.clearPreparedExports()

            assertFalse(exportRoot.exists())
        }

    @Test
    fun `clearing prepared exports removes nested files and succeeds repeatedly`() =
        runTest {
            val exportDirectory = File(exportRoot, "export_test")
            assertTrue(exportDirectory.mkdirs())
            File(exportDirectory, "battery.csv").writeText("timestamp,level\n100,50")
            val unrelatedFile = temporaryFolder.newFile("unrelated.txt")

            repository.clearPreparedExports()
            repository.clearPreparedExports()

            assertFalse(exportRoot.exists())
            assertTrue(unrelatedFile.exists())
        }

    @Test
    fun `failed recursive deletion throws IOException`() =
        runTest {
            assertTrue(exportRoot.mkdir())
            mockkStatic("kotlin.io.FilesKt__UtilsKt")
            try {
                every { exportRoot.deleteRecursively() } returns false

                val thrown = runCatching { repository.clearPreparedExports() }.exceptionOrNull()

                assertTrue("Expected IOException but was $thrown", thrown is IOException)
                verify(exactly = 1) { exportRoot.deleteRecursively() }
                assertTrue(exportRoot.exists())
            } finally {
                unmockkStatic("kotlin.io.FilesKt__UtilsKt")
            }
        }

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
