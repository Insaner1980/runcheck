package com.runcheck.data.export

import android.content.Context
import androidx.core.content.FileProvider
import com.runcheck.domain.repository.FileExportRepository
import com.runcheck.util.AppDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileExportRepositoryImpl
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val dispatchers: AppDispatchers,
    ) : FileExportRepository {
        private val cacheMutex = Mutex()

        override suspend fun prepareExportShare(files: Map<String, String>): List<String> =
            withContext(dispatchers.io) {
                cacheMutex.withLock {
                    val exportRoot =
                        File(context.cacheDir, EXPORT_DIR_NAME).apply {
                            check(mkdirs() || isDirectory) { "Could not create export cache directory" }
                        }
                    val exportId = UUID.randomUUID().toString()
                    val stagingDir = File(exportRoot, ".staging_$exportId")
                    val exportDir = File(exportRoot, "export_$exportId")
                    var exportPrepared = false

                    try {
                        check(stagingDir.mkdir()) { "Could not create export staging directory" }
                        files.forEach { (fileName, content) ->
                            requireSafeExportFileName(fileName)
                            File(stagingDir, fileName).writeText(content, Charsets.UTF_8)
                        }
                        Files.move(
                            stagingDir.toPath(),
                            exportDir.toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                        )
                        val staleExportCutoffMillis = System.currentTimeMillis() - STALE_EXPORT_TTL_MS
                        exportRoot.listFiles()?.forEach { cached ->
                            if (cached != exportDir && cached.lastModified() < staleExportCutoffMillis) {
                                cached.deleteRecursively()
                            }
                        }
                        val exportUris =
                            files.keys.map { fileName ->
                                FileProvider
                                    .getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        File(exportDir, fileName),
                                    ).toString()
                            }
                        exportPrepared = true
                        exportUris
                    } finally {
                        if (!exportPrepared) {
                            stagingDir.deleteRecursively()
                            exportDir.deleteRecursively()
                        }
                    }
                }
            }

        override suspend fun clearPreparedExports() {
            withContext(dispatchers.io) {
                cacheMutex.withLock {
                    File(context.cacheDir, EXPORT_DIR_NAME).deleteRecursively()
                }
            }
        }

        private companion object {
            private const val EXPORT_DIR_NAME = "exports"
            private const val STALE_EXPORT_TTL_MS = 15 * 60 * 1000L
        }
    }

internal fun requireSafeExportFileName(fileName: String) {
    require(
        fileName.isNotBlank() &&
            fileName.endsWith(".csv", ignoreCase = true) &&
            fileName != ".csv" &&
            '/' !in fileName &&
            '\\' !in fileName &&
            fileName != "." &&
            fileName != "..",
    ) { "Export filename must be a single CSV filename" }
}
