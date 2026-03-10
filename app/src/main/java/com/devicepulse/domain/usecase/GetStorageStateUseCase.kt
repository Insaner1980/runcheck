package com.devicepulse.domain.usecase

import com.devicepulse.data.storage.StorageRepository
import com.devicepulse.domain.model.StorageState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageStateUseCase @Inject constructor(
    private val storageRepository: StorageRepository
) {
    operator fun invoke(): Flow<StorageState> = storageRepository.getStorageState()
}
