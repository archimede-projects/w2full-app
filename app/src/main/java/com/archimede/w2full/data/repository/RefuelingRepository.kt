package com.archimede.w2full.data.repository

import com.archimede.w2full.data.local.RifornimentoDao
import com.archimede.w2full.data.local.RifornimentoEntity
import com.archimede.w2full.data.local.VehicleDao
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.domain.calculation.FuelMetrics
import com.archimede.w2full.domain.calculation.FuelMetricsCalculator
import com.archimede.w2full.domain.model.Rifornimento
import com.archimede.w2full.domain.model.RifornimentoDraft
import com.archimede.w2full.domain.model.VehicleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class RefuelingRepository(
    private val vehicleDao: VehicleDao,
    private val rifornimentoDao: RifornimentoDao,
) {
    fun observeRefuelings(
        vehicleId: Long = Rifornimento.DEFAULT_VEHICLE_ID,
    ): Flow<List<Rifornimento>> =
        rifornimentoDao.observeAllForVehicle(vehicleId).map { entries ->
            entries.map(RifornimentoEntity::toDomain)
        }

    fun observeVehicle(
        vehicleId: Long = Rifornimento.DEFAULT_VEHICLE_ID,
    ): Flow<VehicleConfig?> = vehicleDao.observeById(vehicleId).map { it?.toDomain() }

    fun observeMetrics(
        vehicleId: Long = Rifornimento.DEFAULT_VEHICLE_ID,
    ): Flow<FuelMetrics> = combine(
        observeRefuelings(vehicleId),
        observeVehicle(vehicleId),
    ) { refuelings, vehicle ->
        FuelMetricsCalculator.calculate(
            records = refuelings,
            tankCapacityMilliliters = vehicle?.tankCapacityMilliliters,
        )
    }

    suspend fun ensureDefaultVehicle() {
        vehicleDao.insertIfAbsent(
            VehicleEntity(
                id = Rifornimento.DEFAULT_VEHICLE_ID,
                name = "Veicolo",
                defaultFuelType = "Benzina",
                tankCapacityMilliliters = null,
            ),
        )
    }

    suspend fun updateTankCapacityMilliliters(capacityMilliliters: Long?) {
        require(capacityMilliliters == null || capacityMilliliters > 0) {
            "La capacità del serbatoio deve essere positiva."
        }
        ensureDefaultVehicle()
        check(
            vehicleDao.updateTankCapacity(
                Rifornimento.DEFAULT_VEHICLE_ID,
                capacityMilliliters,
            ) == 1,
        ) { "Veicolo V1 non disponibile." }
    }

    suspend fun insert(draft: RifornimentoDraft): Long {
        ensureDefaultVehicle()
        val candidate = draft.toEntity()
        validateCandidate(candidate, rifornimentoDao.getAllForVehicle(draft.vehicleId))
        return rifornimentoDao.insert(candidate)
    }

    suspend fun update(refueling: Rifornimento) {
        require(refueling.id > 0) { "L'id del rifornimento deve essere valorizzato." }
        val existing = rifornimentoDao.getAllForVehicle(refueling.vehicleId)
        require(existing.any { it.id == refueling.id }) { "Rifornimento inesistente." }
        val candidate = refueling.toEntity()
        validateCandidate(candidate, existing.filterNot { it.id == refueling.id })
        check(rifornimentoDao.update(candidate) == 1) { "Aggiornamento non riuscito." }
    }

    suspend fun delete(id: Long) {
        require(id > 0) { "L'id del rifornimento deve essere valorizzato." }
        check(rifornimentoDao.deleteById(id) == 1) { "Rifornimento inesistente." }
    }

    private fun validateCandidate(
        candidate: RifornimentoEntity,
        existing: List<RifornimentoEntity>,
    ) {
        require(candidate.vehicleId > 0) { "vehicleId deve essere positivo." }
        require(candidate.timestampEpochMillis > 0) { "La data deve essere valida." }
        require(candidate.odometerKm >= 0) { "L'odometro non può essere negativo." }
        require(candidate.litersMilliliters > 0) { "I litri devono essere positivi." }
        require(candidate.totalCostCents > 0) { "Il costo deve essere positivo." }
        require(candidate.fuelType.isNotBlank()) { "Il tipo carburante è obbligatorio." }

        validateTimeline(existing + candidate)
    }

    internal fun validateTimeline(entries: List<RifornimentoEntity>) {
        var previousTimestampMaxOdometer: Long? = null
        entries.groupBy { it.timestampEpochMillis }
            .toSortedMap()
            .forEach { (_, sameTimestampEntries) ->
                val currentMinOdometer = sameTimestampEntries.minOf { it.odometerKm }
                val currentMaxOdometer = sameTimestampEntries.maxOf { it.odometerKm }
                require(
                    previousTimestampMaxOdometer == null ||
                        currentMinOdometer >= previousTimestampMaxOdometer!!,
                ) { "L'odometro non può diminuire nel tempo." }
                previousTimestampMaxOdometer = currentMaxOdometer
            }
    }
}

private fun RifornimentoDraft.toEntity() = RifornimentoEntity(
    vehicleId = vehicleId,
    timestampEpochMillis = timestampEpochMillis,
    odometerKm = odometerKm,
    litersMilliliters = litersMilliliters,
    totalCostCents = totalCostCents,
    fuelType = fuelType.trim(),
    isFullTank = isFullTank,
)

private fun Rifornimento.toEntity() = RifornimentoEntity(
    id = id,
    vehicleId = vehicleId,
    timestampEpochMillis = timestampEpochMillis,
    odometerKm = odometerKm,
    litersMilliliters = litersMilliliters,
    totalCostCents = totalCostCents,
    fuelType = fuelType.trim(),
    isFullTank = isFullTank,
)

private fun RifornimentoEntity.toDomain() = Rifornimento(
    id = id,
    vehicleId = vehicleId,
    timestampEpochMillis = timestampEpochMillis,
    odometerKm = odometerKm,
    litersMilliliters = litersMilliliters,
    totalCostCents = totalCostCents,
    fuelType = fuelType,
    isFullTank = isFullTank,
)

private fun VehicleEntity.toDomain() = VehicleConfig(
    id = id,
    name = name,
    defaultFuelType = defaultFuelType,
    tankCapacityMilliliters = tankCapacityMilliliters,
)
