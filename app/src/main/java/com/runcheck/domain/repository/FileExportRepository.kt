package com.runcheck.domain.repository

interface FileExportRepository {
    suspend fun prepareExportShare(files: Map<String, String>): List<String>
}
