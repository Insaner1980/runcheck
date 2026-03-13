package com.devicepulse.domain.repository

import com.devicepulse.domain.model.DeviceProfileInfo
import kotlinx.coroutines.flow.Flow

interface DeviceProfileRepository {
    fun getProfile(): Flow<DeviceProfileInfo?>
    suspend fun getProfileSync(): DeviceProfileInfo?
    suspend fun ensureProfile(): DeviceProfileInfo
    suspend fun refreshProfile(): DeviceProfileInfo
}
