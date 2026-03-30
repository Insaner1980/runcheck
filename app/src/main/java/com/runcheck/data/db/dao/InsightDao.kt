package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.runcheck.data.db.entity.InsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {
    @Query(
        """
        SELECT * FROM insights
        WHERE dismissed = 0
        ORDER BY priority ASC, confidence DESC, generated_at DESC
    """,
    )
    fun observeUndismissedInsights(): Flow<List<InsightEntity>>

    @Query("SELECT * FROM insights WHERE rule_id = :ruleId")
    suspend fun getByRule(ruleId: String): List<InsightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<InsightEntity>)

    @Query("DELETE FROM insights WHERE expires_at <= :now")
    suspend fun deleteExpired(now: Long)

    @Query("DELETE FROM insights WHERE rule_id = :ruleId")
    suspend fun deleteByRule(ruleId: String)

    @Query("DELETE FROM insights WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE insights SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)

    @Query("UPDATE insights SET seen = 1 WHERE seen = 0")
    suspend fun markAllSeen()

    @Query("SELECT COUNT(*) FROM insights WHERE dismissed = 0 AND expires_at > :now")
    suspend fun countActive(now: Long): Int

    @Query("DELETE FROM insights")
    suspend fun deleteAll()
}
