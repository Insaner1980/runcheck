package com.runcheck.domain.usecase

import com.runcheck.domain.model.StorageState
import com.runcheck.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStorageStateUseCase
    @Inject
    constructor(
        private val storageRepository: StorageRepository,
    ) {
        operator fun invoke(): Flow<StorageState> = storageRepository.getStorageState()
    }
