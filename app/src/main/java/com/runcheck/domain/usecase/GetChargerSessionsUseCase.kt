package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ChargingSession
import com.devicepulse.domain.repository.ChargerRepository
import com.devicepulse.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetChargerSessionsUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val proStatusProvider: ProStatusProvider
) {
    operator fun invoke(): Flow<List<ChargingSession>> =
        if (proStatusProvider.isPro()) {
            chargerRepository.getAllSessions()
        } else {
            flowOf(emptyList())
        }
}
