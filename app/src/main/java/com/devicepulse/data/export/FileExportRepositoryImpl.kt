package com.devicepulse.data.export

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.Build
import android.provider.MediaStore
import com.devicepulse.domain.repository.FileExportRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileExportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : FileExportRepository {

    override suspend fun exportToDownloads(files: Map<String, String>): Boolean =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            var exportedAnyFile = false

            for ((fileName, content) in files) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                }
                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext false

                val wroteFile = resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                    true
                } ?: false

                if (!wroteFile) {
                    resolver.delete(uri, null, null)
                    return@withContext false
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply {
                            put(MediaStore.Downloads.IS_PENDING, 0)
                        },
                        null,
                        null
                    )
                }

                exportedAnyFile = true
            }

            exportedAnyFile
        }
}
