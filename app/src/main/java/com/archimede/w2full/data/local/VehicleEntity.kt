package com.archimede.w2full.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    @ColumnInfo(name = "default_fuel_type")
    val defaultFuelType: String,
    @ColumnInfo(name = "tank_capacity_milliliters")
    val tankCapacityMilliliters: Long?,
)
