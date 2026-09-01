package com.archimede.w2full.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "refuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(
            name = "idx_refuel_vehicle_odometer",
            value = ["vehicle_id", "odometer_km"],
        ),
        Index(
            name = "idx_refuel_vehicle_timestamp",
            value = ["vehicle_id", "timestamp_epoch_millis"],
        ),
    ],
)
data class RifornimentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long,
    @ColumnInfo(name = "timestamp_epoch_millis")
    val timestampEpochMillis: Long,
    @ColumnInfo(name = "odometer_km")
    val odometerKm: Long,
    @ColumnInfo(name = "liters_milliliters")
    val litersMilliliters: Long,
    @ColumnInfo(name = "total_cost_cents")
    val totalCostCents: Long,
    @ColumnInfo(name = "fuel_type")
    val fuelType: String,
    @ColumnInfo(name = "is_full_tank")
    val isFullTank: Boolean,
)
