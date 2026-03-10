package com.devicepulse.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devicepulse.data.db.entity.AppBatteryUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBatteryUsageDao {

    @Insert
    suspend fun insert(usage: AppBatteryUsageEntity)

    @Insert
    suspend fun insertAll(usages: List<AppBatteryUsageEntity>)

    @Query("""
        SELECT package_name, app_label,
               SUM(foreground_time_ms) as foreground_time_ms,
               SUM(estimated_drain_mah) as estimated_drain_mah,
               MAX(timestamp) as timestamp,
               0 as id
        FROM app_battery_usage
        WHERE timestamp >= :since
        GROUP BY package_name
        ORDER BY foreground_time_ms DESC
    """)
    fun getAggregatedUsageSince(since: Long): Flow<List<AppBatteryUsageEntity>>

    @Query("SELECT * FROM app_battery_usage WHERE timestamp >= :since ORDER BY foreground_time_ms DESC")
    fun getUsageSince(since: Long): Flow<List<AppBatteryUsageEntity>>

    @Query("DELETE FROM app_battery_usage WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
