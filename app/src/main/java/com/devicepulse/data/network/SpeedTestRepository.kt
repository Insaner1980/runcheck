package com.devicepulse.data.network

import com.devicepulse.data.db.dao.SpeedTestResultDao
import com.devicepulse.data.db.entity.SpeedTestResultEntity
import com.devicepulse.domain.model.ConnectionType
import com.devicepulse.domain.model.SpeedTestResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpeedTestRepository @Inject constructor(
    private val speedTestService: SpeedTestService,
    private val speedTestResultDao: SpeedTestResultDao
) {

    fun runSpeedTest(): Flow<SpeedTestService.SpeedTestProgress> =
        speedTestService.runSpeedTest()

    suspend fun saveResult(result: SpeedTestResult) {
        val entity = SpeedTestResultEntity(
            timestamp = result.timestamp,
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

    fun getLatestResult(): Flow<SpeedTestResult?> =
        speedTestResultDao.getLatestResult().map { it?.toDomain() }

    fun getRecentResults(limit: Int): Flow<List<SpeedTestResult>> =
        speedTestResultDao.getRecentResults(limit).map { list ->
            list.map { it.toDomain() }
        }

    suspend fun trimResults(keepCount: Int) {
        speedTestResultDao.deleteOldResults(keepCount)
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
