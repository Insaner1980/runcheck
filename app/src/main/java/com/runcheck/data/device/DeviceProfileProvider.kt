package com.runcheck.data.device

/**
 * Data-layer interface for accessing the raw [DeviceProfile].
 * Used by data-layer callers (repositories, data source factories) that need
 * fields specific to the data-layer profile, avoiding direct dependency
 * on [DeviceProfileRepositoryImpl].
 */
interface DeviceProfileProvider {
    suspend fun getDeviceProfile(): DeviceProfile
}
