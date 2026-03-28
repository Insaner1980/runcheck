package com.runcheck.data.device

import android.os.Build
import com.google.gson.Gson
import com.runcheck.data.db.dao.DeviceDao
import com.runcheck.data.db.entity.DeviceEntity
import com.runcheck.domain.model.DeviceProfileInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.runcheck.domain.repository.DeviceProfileRepository as DeviceProfileRepositoryContract

@Singleton
class DeviceProfileRepositoryImpl
    @Inject
    constructor(
        private val deviceDao: DeviceDao,
        private val capabilityManager: DeviceCapabilityManager,
        private val gson: Gson,
    ) : DeviceProfileRepositoryContract,
        DeviceProfileProvider {
        override fun getProfile(): Flow<DeviceProfileInfo?> =
            deviceDao.getDevice().map { entity ->
                entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java)?.toDomain() }
            }

        override suspend fun getProfileSync(): DeviceProfileInfo? {
            val entity = deviceDao.getDeviceSync()
            return entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java)?.toDomain() }
        }

        override suspend fun refreshProfile(): DeviceProfileInfo {
            val profile = capabilityManager.detectCapabilities()
            val existing = deviceDao.getDeviceSync()
            val now = System.currentTimeMillis()
            val entity =
                DeviceEntity(
                    id = profile.deviceId,
                    manufacturer = profile.manufacturer,
                    model = profile.model,
                    apiLevel = profile.apiLevel,
                    firstSeen = existing?.firstSeen ?: now,
                    profileJson = gson.toJson(profile),
                )
            deviceDao.insertOrUpdate(entity)
            deviceDao.deleteAllExcept(entity.id)
            return profile.toDomain()
        }

        override suspend fun ensureProfile(): DeviceProfileInfo {
            val existing = getProfileSync()
            if (existing != null && existing.apiLevel == Build.VERSION.SDK_INT) {
                return existing
            }
            return refreshProfile()
        }

        override suspend fun getDeviceProfile(): DeviceProfile {
            val entity = deviceDao.getDeviceSync()
            val profile = entity?.let { gson.fromJson(it.profileJson, DeviceProfile::class.java) }
            return profile ?: capabilityManager.detectCapabilities().also { detected ->
                val existing = deviceDao.getDeviceSync()
                val now = System.currentTimeMillis()
                val deviceEntity =
                    DeviceEntity(
                        id = detected.deviceId,
                        manufacturer = detected.manufacturer,
                        model = detected.model,
                        apiLevel = detected.apiLevel,
                        firstSeen = existing?.firstSeen ?: now,
                        profileJson = gson.toJson(detected),
                    )
                deviceDao.insertOrUpdate(deviceEntity)
                deviceDao.deleteAllExcept(deviceEntity.id)
            }
        }
    }

private fun DeviceProfile.toDomain() =
    DeviceProfileInfo(
        manufacturer = manufacturer,
        model = model,
        apiLevel = apiLevel,
        currentNowReliable = currentNowReliable,
        currentNowUnit = currentNowUnit,
        currentNowSignConvention = currentNowSignConvention,
        cycleCountAvailable = cycleCountAvailable,
        thermalZonesAvailable = thermalZonesAvailable,
        storageHealthAvailable = storageHealthAvailable,
    )
