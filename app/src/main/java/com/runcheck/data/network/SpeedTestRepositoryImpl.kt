package com.runcheck.data.network

import com.runcheck.data.db.dao.SpeedTestResultDao
import com.runcheck.data.db.entity.SpeedTestResultEntity
import com.runcheck.domain.model.ConnectionType
import com.runcheck.domain.model.SpeedTestProgress
import com.runcheck.domain.model.SpeedTestResult
import com.runcheck.domain.repository.SpeedTestRepository as SpeedTestRepositoryContract
import com.runcheck.util.TimestampSanitizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedTestRepositoryImpl @Inject constructor(
    private val speedTestService: SpeedTestService,
    private val speedTestResultDao: SpeedTestResultDao
) : SpeedTestRepositoryContract {

    override fun runSpeedTest(): Flow<SpeedTestProgress> =
        speedTestService.runSpeedTest().map { it.toDomain() }

    override suspend fun saveResult(result: SpeedTestResult) {
        val entity = SpeedTestResultEntity(
            timestamp = TimestampSanitizer.clampToNow(result.timestamp),
            downloadMbps = result.downloadMbps,
            uploadMbps = result.uploadMbps,
            pingMs = result.pingMs,
            jitterMs = result.jitterMs,
            serverName = result.serverName,
            serverLocation = result.serverLocation,
            connectionType = result.connectionType.name,
            networkSubtype = result.networkSubtype,
            signalDbm = result.signalDbm
        )
        speedTestResultDao.insert(entity)
    }

    override fun getLatestResult(): Flow<SpeedTestResult?> =
        speedTestResultDao.getLatestResult(System.currentTimeMillis()).map { entity ->
            entity?.toDomain()?.takeIf { TimestampSanitizer.isUsable(it.timestamp) }
        }

    override fun getRecentResults(limit: Int): Flow<List<SpeedTestResult>> =
        speedTestResultDao.getRecentResults(limit, System.currentTimeMillis()).map { list ->
            list.map { it.toDomain() }
                .filter { TimestampSanitizer.isUsable(it.timestamp) }
        }

    override suspend fun trimResults(keepCount: Int) {
        speedTestResultDao.deleteOldResults(keepCount)
    }

    override suspend fun deleteOlderThan(cutoff: Long) {
        speedTestResultDao.deleteOlderThan(cutoff)
    }

    override suspend fun deleteAll() {
        speedTestResultDao.deleteAll()
    }

    private fun SpeedTestResultEntity.toDomain(): SpeedTestResult = SpeedTestResult(
        id = id,
        timestamp = timestamp,
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        pingMs = pingMs,
        jitterMs = jitterMs,
        serverName = serverName,
        serverLocation = serverLocation,
        connectionType = try {
            ConnectionType.valueOf(connectionType)
        } catch (_: IllegalArgumentException) {
            ConnectionType.NONE
        },
        networkSubtype = networkSubtype,
        signalDbm = signalDbm
    )
}

private fun SpeedTestService.SpeedTestProgress.toDomain(): SpeedTestProgress = when (this) {
    is SpeedTestService.SpeedTestProgress.PingPhase ->
        SpeedTestProgress.PingPhase(pingMs, jitterMs)
    is SpeedTestService.SpeedTestProgress.DownloadPhase ->
        SpeedTestProgress.DownloadPhase(currentMbps, progress)
    is SpeedTestService.SpeedTestProgress.UploadPhase ->
        SpeedTestProgress.UploadPhase(currentMbps, progress)
    is SpeedTestService.SpeedTestProgress.Completed ->
        SpeedTestProgress.Completed(
            downloadMbps,
            uploadMbps,
            pingMs,
            jitterMs,
            serverName,
            serverLocation
        )
    is SpeedTestService.SpeedTestProgress.Failed ->
        SpeedTestProgress.Failed(error)
}
