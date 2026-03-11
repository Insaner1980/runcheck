package com.devicepulse.domain.repository

interface FileExportRepository {
    suspend fun exportToDownloads(files: Map<String, String>): Boolean
}
