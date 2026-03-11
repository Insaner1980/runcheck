package com.devicepulse.domain.usecase

import com.devicepulse.domain.model.ChargerSummary
import com.devicepulse.domain.repository.ChargerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetChargerComparisonUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository
) {

    operator fun invoke(): Flow<List<ChargerSummary>> {
        return combine(
            chargerRepository.getChargerProfiles(),
            chargerRepository.getAllSessions()
        ) { chargers, sessions ->
            chargers.map { charger ->
                val chargerSessions = sessions.filter { it.chargerId == charger.id }
                val completedSessions = chargerSessions.filter { it.endTime != null }

                val avgSpeed = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { it.avgCurrentMa }.let { currents ->
                        if (currents.isNotEmpty()) currents.average().toInt() else null
                    }
                } else null

                val avgTimeToFull = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { session ->
                        session.endTime?.let { end ->
                            val durationMinutes = (end - session.startTime) / 60_000
                            val levelGain = (session.endLevel ?: session.startLevel) - session.startLevel
                            if (levelGain > 0) {
                                (durationMinutes * 100 / levelGain).toInt()
                            } else null
                        }
                    }.let { times ->
                        if (times.isNotEmpty()) times.average().toInt() else null
                    }
                } else null

                val lastUsed = chargerSessions.maxByOrNull { it.startTime }?.startTime

                ChargerSummary(
                    chargerId = charger.id,
                    chargerName = charger.name,
                    sessionCount = chargerSessions.size,
                    avgChargingSpeedMa = avgSpeed,
                    avgTimeToFullMinutes = avgTimeToFull,
                    lastUsed = lastUsed
                )
            }.sortedByDescending { it.lastUsed ?: 0L }
        }
    }
}
