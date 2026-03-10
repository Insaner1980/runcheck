package com.devicepulse.data.device

import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.entity.DeviceEntity
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceProfileRepository @Inject constructor(
    private val deviceDao: DeviceDao,
    private val capabilityManager: DeviceCapabilityManager,
    private val gson: Gson
) {

    fun getProfile(): Flow<DeviceProfile?> {
        return deviceDao.getDevice().map { entity ->
            entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java) }
        }
    }

    suspend fun getProfileSync(): DeviceProfile? {
        val entity = deviceDao.getDeviceSync()
        return entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java) }
    }

    suspend fun refreshProfile(): DeviceProfile {
        val profile = capabilityManager.detectCapabilities()
        val entity = DeviceEntity(
            id = profile.deviceId,
            manufacturer = profile.manufacturer,
            model = profile.model,
            apiLevel = profile.apiLevel,
            firstSeen = System.currentTimeMillis(),
            profileJson = gson.toJson(profile)
        )
        deviceDao.insertOrUpdate(entity)
        return profile
    }

    suspend fun ensureProfile(): DeviceProfile {
        return getProfileSync() ?: refreshProfile()
    }
}
