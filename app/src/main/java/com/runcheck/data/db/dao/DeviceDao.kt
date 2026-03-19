package com.runcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.runcheck.data.db.entity.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(device: DeviceEntity)

    @Query("DELETE FROM devices WHERE id != :currentId")
    suspend fun deleteAllExcept(currentId: String)

    @Query("SELECT * FROM devices ORDER BY first_seen DESC, id DESC LIMIT 1")
    fun getDevice(): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices ORDER BY first_seen DESC, id DESC LIMIT 1")
    suspend fun getDeviceSync(): DeviceEntity?
}
