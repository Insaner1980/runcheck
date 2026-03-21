package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetChargerComparisonUseCase @Inject constructor(
    private val chargerRepository: ChargerRepository,
    private val proStatusProvider: ProStatusProvider
) {

    operator fun invoke(): Flow<List<ChargerSummary>> {
        if (!proStatusProvider.isPro()) {
            return flowOf(emptyList())
        }
        return combine(
            chargerRepository.getChargerProfiles(),
            chargerRepository.getAllSessions()
        ) { chargers, sessions ->
            chargers.map { charger ->
                val chargerSessions = sessions.filter { it.chargerId == charger.id }
                val completedSessions = chargerSessions.filter { it.endTime != null }
                val latestCompletedSession = completedSessions.maxByOrNull { it.endTime ?: 0L }

                val avgSpeed = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { it.avgCurrentMa }.averageOrNull()
                } else null

                val avgPower = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { session ->
                        session.avgPowerMw ?: session.avgCurrentMa?.let { currentMa ->
                            session.avgVoltageMv?.let { voltageMv ->
                                (currentMa * voltageMv) / 1000
                            }
                        }
                    }.averageOrNull()
                } else null

                val latestPower = latestCompletedSession?.avgPowerMw ?: latestCompletedSession?.avgCurrentMa?.let { currentMa ->
                    latestCompletedSession.avgVoltageMv?.let { voltageMv ->
                        (currentMa * voltageMv) / 1000
                    }
                }

                val avgTimeToFull = if (completedSessions.isNotEmpty()) {
                    completedSessions.mapNotNull { session ->
                        session.endTime?.let { end ->
                            val durationMinutes = (end - session.startTime) / 60_000
                            val levelGain = (session.endLevel ?: session.startLevel) - session.startLevel
                            if (levelGain > 0) {
                                (durationMinutes * 100 / levelGain).toInt()
                            } else null
                        }
                    }.averageOrNull()
                } else null

                val lastUsed = latestCompletedSession?.endTime ?: chargerSessions.maxByOrNull { it.startTime }?.startTime

                ChargerSummary(
                    chargerId = charger.id,
                    chargerName = charger.name,
                    sessionCount = chargerSessions.size,
                    avgChargingSpeedMa = avgSpeed,
                    avgPowerMw = avgPower,
                    latestChargingSpeedMa = latestCompletedSession?.avgCurrentMa,
                    latestPowerMw = latestPower,
                    avgTimeToFullMinutes = avgTimeToFull,
                    lastUsed = lastUsed,
                    hasActiveSession = chargerSessions.any { it.endTime == null }
                )
            }.sortedByDescending { it.lastUsed ?: 0L }
        }
    }
}

private fun List<Int>.averageOrNull(): Int? =
    takeIf(List<Int>::isNotEmpty)?.average()?.toInt()
