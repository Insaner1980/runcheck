package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.runcheck.data.db.entity.ChargerProfileEntity
import com.runcheck.data.db.entity.ChargingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargerDao {

    @Insert
    suspend fun insertCharger(charger: ChargerProfileEntity): Long

    @Delete
    suspend fun deleteCharger(charger: ChargerProfileEntity)

    @Query("DELETE FROM charger_profiles WHERE id = :id")
    suspend fun deleteChargerById(id: Long)

    @Insert
    suspend fun insertSession(session: ChargingSessionEntity): Long

    @Query("SELECT * FROM charger_profiles ORDER BY created DESC")
    fun getChargerProfiles(): Flow<List<ChargerProfileEntity>>

    @Query("SELECT * FROM charging_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<ChargingSessionEntity>>

    @Query("SELECT * FROM charging_sessions WHERE end_time IS NULL LIMIT 1")
    suspend fun getActiveSession(): ChargingSessionEntity?

    @Query("DELETE FROM charging_sessions WHERE end_time IS NOT NULL AND end_time < :cutoff")
    suspend fun deleteSessionsOlderThan(cutoff: Long)

    @Query(
        """UPDATE charging_sessions
           SET end_time = :endTime,
               end_level = :endLevel,
               avg_current_ma = :avgCurrentMa,
               max_current_ma = :maxCurrentMa,
               avg_voltage_mv = :avgVoltageMv,
               avg_power_mw = :avgPowerMw
           WHERE id = :id"""
    )
    suspend fun completeSession(
        id: Long,
        endTime: Long,
        endLevel: Int,
        avgCurrentMa: Int?,
        maxCurrentMa: Int?,
        avgVoltageMv: Int?,
        avgPowerMw: Int?
    )
}
