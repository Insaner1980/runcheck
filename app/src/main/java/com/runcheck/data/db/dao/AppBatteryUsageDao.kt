package com.runcheck.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.runcheck.data.db.entity.AppBatteryUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppBatteryUsageDao {
    @Insert
    suspend fun insert(usage: AppBatteryUsageEntity)

    @Insert
    suspend fun insertAll(usages: List<AppBatteryUsageEntity>)

    @Query(
        """
        SELECT
            usage.package_name as package_name,
            (
                SELECT latest.app_label
                FROM app_battery_usage AS latest
                WHERE latest.package_name = usage.package_name
                  AND latest.timestamp >= :since
                ORDER BY latest.timestamp DESC, latest.id DESC
                LIMIT 1
            ) as app_label,
            SUM(usage.foreground_time_ms) as foreground_time_ms,
            SUM(usage.estimated_drain_mah) as estimated_drain_mah,
            MAX(usage.timestamp) as timestamp,
            0 as id
        FROM app_battery_usage AS usage
        WHERE usage.timestamp >= :since
        GROUP BY usage.package_name
        ORDER BY foreground_time_ms DESC
    """,
    )
    fun getAggregatedUsageSince(since: Long): PagingSource<Int, AppBatteryUsageEntity>

    @Query(
        """
        SELECT
            COALESCE(SUM(package_total), 0) AS total_foreground_time_ms,
            COALESCE(MAX(package_total), 0) AS max_foreground_time_ms
        FROM (
            SELECT SUM(foreground_time_ms) AS package_total
            FROM app_battery_usage
            WHERE timestamp >= :since
            GROUP BY package_name
        )
    """,
    )
    fun getUsageSummarySince(since: Long): Flow<AppBatteryUsageSummaryRow>

    @Query("SELECT * FROM app_battery_usage WHERE timestamp >= :since ORDER BY foreground_time_ms DESC")
    fun getUsageSince(since: Long): Flow<List<AppBatteryUsageEntity>>

    @Query("DELETE FROM app_battery_usage WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM app_battery_usage")
    suspend fun deleteAll()
}

data class AppBatteryUsageSummaryRow(
    val total_foreground_time_ms: Long,
    val max_foreground_time_ms: Long,
)
