package com.runcheck.domain.usecase

import com.runcheck.domain.model.ChargerSummary
import com.runcheck.domain.model.ChargingSession
import com.runcheck.domain.repository.ChargerRepository
import com.runcheck.domain.repository.ProStatusProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class GetChargerComparisonUseCase
    @Inject
    constructor(
        private val chargerRepository: ChargerRepository,
        private val proStatusProvider: ProStatusProvider,
    ) {
        operator fun invoke(): Flow<List<ChargerSummary>> {
            if (!proStatusProvider.isPro()) {
                return flowOf(emptyList())
            }
            return combine(
                chargerRepository.getChargerProfiles(),
                chargerRepository.getAllSessions(),
            ) { chargers, sessions ->
                chargers
                    .map { charger ->
                        buildSummary(
                            chargerId = charger.id,
                            chargerName = charger.name,
                            sessions = sessions.filter { it.chargerId == charger.id },
                        )
                    }.sortedByDescending { it.lastUsed ?: 0L }
            }
        }

        private fun buildSummary(
            chargerId: Long,
            chargerName: String,
            sessions: List<ChargingSession>,
        ): ChargerSummary {
            val completedSessions = sessions.filter { it.endTime != null }
            val latestCompletedSession = completedSessions.maxByOrNull { it.endTime ?: 0L }

            return ChargerSummary(
                chargerId = chargerId,
                chargerName = chargerName,
                sessionCount = sessions.size,
                avgChargingSpeedMa = completedSessions.mapNotNull { it.avgCurrentMa }.averageOrNull(),
                avgPowerMw = completedSessions.mapNotNull(::resolveSessionPowerMw).averageOrNull(),
                latestChargingSpeedMa = latestCompletedSession?.avgCurrentMa,
                latestPowerMw = latestCompletedSession?.let(::resolveSessionPowerMw),
                avgTimeToFullMinutes = completedSessions.mapNotNull(::estimateTimeToFullMinutes).averageOrNull(),
                lastUsed = latestCompletedSession?.endTime ?: sessions.maxByOrNull { it.startTime }?.startTime,
                hasActiveSession = sessions.any { it.endTime == null },
            )
        }

        private fun resolveSessionPowerMw(session: ChargingSession): Int? =
            session.avgPowerMw
                ?: session.avgCurrentMa?.let { currentMa ->
                    session.avgVoltageMv?.let { voltageMv ->
                        (currentMa * voltageMv) / 1000
                    }
                }

        private fun estimateTimeToFullMinutes(session: ChargingSession): Int? {
            val endTime = session.endTime ?: return null
            val durationMinutes = (endTime - session.startTime) / 60_000
            val levelGain = (session.endLevel ?: session.startLevel) - session.startLevel
            if (levelGain <= 0) return null
            return (durationMinutes * 100 / levelGain).toInt()
        }
    }

private fun List<Int>.averageOrNull(): Int? = takeIf(List<Int>::isNotEmpty)?.average()?.toInt()
