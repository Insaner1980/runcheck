package com.runcheck.domain.usecase

import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProAccessUseCase @Inject constructor(
    private val proStatusProvider: ProStatusProvider
) {
    operator fun invoke(): Flow<Boolean> = proStatusProvider.isProUser
}
