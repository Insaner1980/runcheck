package com.runcheck.domain.repository

import android.net.Uri

interface FileExportRepository {
    suspend fun prepareExportShare(files: Map<String, String>): List<Uri>
}
