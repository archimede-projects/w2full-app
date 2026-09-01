package com.archimede.w2full.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RifornimentoDao {
    @Query(
        """
        SELECT * FROM refuel_entries
        WHERE vehicle_id = :vehicleId
        ORDER BY odometer_km DESC, timestamp_epoch_millis DESC, id DESC
        """,
    )
    fun observeAllForVehicle(vehicleId: Long): Flow<List<RifornimentoEntity>>

    @Query(
        """
        SELECT * FROM refuel_entries
        WHERE vehicle_id = :vehicleId
        ORDER BY timestamp_epoch_millis ASC, id ASC
        """,
    )
    suspend fun getAllForVehicle(vehicleId: Long): List<RifornimentoEntity>

    @Query("SELECT * FROM refuel_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RifornimentoEntity?

    @Insert
    suspend fun insert(entry: RifornimentoEntity): Long

    @Update
    suspend fun update(entry: RifornimentoEntity): Int

    @Query("DELETE FROM refuel_entries WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
