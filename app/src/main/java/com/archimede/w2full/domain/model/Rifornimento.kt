package com.archimede.w2full.domain.model

data class Rifornimento(
    val id: Long = 0,
    val vehicleId: Long = DEFAULT_VEHICLE_ID,
    val timestampEpochMillis: Long,
    val odometerKm: Long,
    val litersMilliliters: Long,
    val totalCostCents: Long,
    val fuelType: String,
    val isFullTank: Boolean,
) {
    companion object {
        const val DEFAULT_VEHICLE_ID: Long = 1L
    }
}

data class RifornimentoDraft(
    val vehicleId: Long = Rifornimento.DEFAULT_VEHICLE_ID,
    val timestampEpochMillis: Long,
    val odometerKm: Long,
    val litersMilliliters: Long,
    val totalCostCents: Long,
    val fuelType: String,
    val isFullTank: Boolean,
)

data class VehicleConfig(
    val id: Long = Rifornimento.DEFAULT_VEHICLE_ID,
    val name: String,
    val defaultFuelType: String,
    val tankCapacityMilliliters: Long?,
)
