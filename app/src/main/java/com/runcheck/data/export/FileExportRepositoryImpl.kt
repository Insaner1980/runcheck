package com.devicepulse.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.devicepulse.domain.repository.FileExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileExportRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : FileExportRepository {

    override suspend fun prepareExportShare(files: Map<String, String>): List<Uri> =
        withContext(Dispatchers.IO) {
            val exportRoot = File(context.cacheDir, EXPORT_DIR_NAME).apply {
                mkdirs()
            }
            exportRoot.listFiles()?.forEach(File::deleteRecursively)

            val exportDir = File(exportRoot, "export_${System.currentTimeMillis()}").apply {
                mkdirs()
            }

            files.map { (fileName, content) ->
                val file = File(exportDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
        }

    private companion object {
        private const val EXPORT_DIR_NAME = "exports"
    }
}
