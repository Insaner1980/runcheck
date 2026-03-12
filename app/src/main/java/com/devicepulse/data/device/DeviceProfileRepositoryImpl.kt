package com.devicepulse.data.device

import com.devicepulse.data.db.dao.DeviceDao
import com.devicepulse.data.db.entity.DeviceEntity
import com.devicepulse.domain.model.DeviceProfileInfo
import com.devicepulse.domain.repository.DeviceProfileRepository as DeviceProfileRepositoryContract
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceProfileRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao,
    private val capabilityManager: DeviceCapabilityManager,
    private val gson: Gson
) : DeviceProfileRepositoryContract {

    override fun getProfile(): Flow<DeviceProfileInfo?> {
        return deviceDao.getDevice().map { entity ->
            entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java)?.toDomain() }
        }
    }

    override suspend fun getProfileSync(): DeviceProfileInfo? {
        val entity = deviceDao.getDeviceSync()
        return entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java)?.toDomain() }
    }

    override suspend fun refreshProfile(): DeviceProfileInfo {
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
        return profile.toDomain()
    }

    override suspend fun ensureProfile(): DeviceProfileInfo {
        return getProfileSync() ?: refreshProfile()
    }

    /**
     * Internal method for data-layer callers that need the raw DeviceProfile
     * (e.g., BatteryDataSourceFactory which depends on data-layer specific fields).
     */
    internal suspend fun ensureProfileInternal(): DeviceProfile {
        val entity = deviceDao.getDeviceSync()
        val profile = entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java) }
        return profile ?: capabilityManager.detectCapabilities().also { detected ->
            val deviceEntity = DeviceEntity(
                id = detected.deviceId,
                manufacturer = detected.manufacturer,
                model = detected.model,
                apiLevel = detected.apiLevel,
                firstSeen = System.currentTimeMillis(),
                profileJson = gson.toJson(detected)
            )
            deviceDao.insertOrUpdate(deviceEntity)
        }
    }
}

private fun DeviceProfile.toDomain() = DeviceProfileInfo(
    manufacturer = manufacturer,
    model = model,
    apiLevel = apiLevel,
    currentNowReliable = currentNowReliable,
    currentNowUnit = currentNowUnit,
    currentNowSignConvention = currentNowSignConvention,
    cycleCountAvailable = cycleCountAvailable,
    batteryHealthPercentAvailable = batteryHealthPercentAvailable,
    thermalZonesAvailable = thermalZonesAvailable,
    storageHealthAvailable = storageHealthAvailable
)
