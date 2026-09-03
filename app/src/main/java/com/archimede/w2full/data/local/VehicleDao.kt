package com.archimede.w2full.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(vehicle: VehicleEntity): Long

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    suspend fun getById(vehicleId: Long): VehicleEntity?

    @Query("SELECT * FROM vehicles WHERE id = :vehicleId LIMIT 1")
    fun observeById(vehicleId: Long): Flow<VehicleEntity?>

    @Query("UPDATE vehicles SET tank_capacity_milliliters = :capacityMilliliters WHERE id = :vehicleId")
    suspend fun updateTankCapacity(vehicleId: Long, capacityMilliliters: Long?): Int

    @Query("UPDATE vehicles SET default_fuel_type = :fuelType WHERE id = :vehicleId")
    suspend fun updateDefaultFuelType(vehicleId: Long, fuelType: String): Int
}
