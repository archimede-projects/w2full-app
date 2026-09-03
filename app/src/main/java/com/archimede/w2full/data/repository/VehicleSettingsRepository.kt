package com.archimede.w2full.data.repository

import com.archimede.w2full.data.local.MimitCacheDao
import com.archimede.w2full.data.local.VehicleDao
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.mimit.MimitStationPriceSelector
import com.archimede.w2full.domain.model.Rifornimento
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class VehicleFuelSettings(
    val vehicleName: String,
    val selectedFuelType: String,
    val fuelOptions: List<String>,
)

sealed interface VehicleFuelUpdateResult {
    data class Success(val fuelType: String) : VehicleFuelUpdateResult
    data object Failure : VehicleFuelUpdateResult
}

interface VehicleSettingsRepository {
    fun observeSettings(): Flow<VehicleFuelSettings>

    suspend fun setDefaultFuelType(fuelType: String): VehicleFuelUpdateResult
}

object VehicleFuelOptions {
    val BASE_FUELS = listOf("Benzina", "Gasolio", "GPL", "Metano")

    private val whitespace = Regex("\\s+")

    fun compact(value: String?): String = value
        ?.trim()
        ?.replace(whitespace, " ")
        .orEmpty()

    fun normalize(value: String?): String = compact(value).lowercase(Locale.ROOT)

    fun same(left: String?, right: String?): Boolean = normalize(left) == normalize(right)

    fun build(
        currentFuelType: String?,
        cachedFuelDescriptions: List<String>,
    ): List<String> {
        val labelsByNormalizedValue = linkedMapOf<String, String>()

        fun add(value: String?) {
            val compact = compact(value)
            if (compact.isBlank()) return
            labelsByNormalizedValue.putIfAbsent(normalize(compact), compact)
        }

        BASE_FUELS.forEach(::add)
        cachedFuelDescriptions.forEach(::add)
        add(currentFuelType)

        return labelsByNormalizedValue.values.sortedWith(
            compareBy<String> { it.lowercase(Locale.ROOT) }.thenBy { it },
        )
    }
}

class RoomVehicleSettingsRepository(
    private val vehicleDao: VehicleDao,
    private val cacheDao: MimitCacheDao,
) : VehicleSettingsRepository {
    override fun observeSettings(): Flow<VehicleFuelSettings> = combine(
        vehicleDao.observeById(Rifornimento.DEFAULT_VEHICLE_ID),
        cacheDao.observeFuelDescriptions(),
    ) { vehicle, cachedFuelDescriptions ->
        val selectedFuelType = VehicleFuelOptions.compact(vehicle?.defaultFuelType)
            .ifBlank { MimitStationPriceSelector.FALLBACK_FUEL_TYPE }
        VehicleFuelSettings(
            vehicleName = VehicleFuelOptions.compact(vehicle?.name).ifBlank { "Veicolo" },
            selectedFuelType = selectedFuelType,
            fuelOptions = VehicleFuelOptions.build(selectedFuelType, cachedFuelDescriptions),
        )
    }

    override suspend fun setDefaultFuelType(fuelType: String): VehicleFuelUpdateResult {
        val compactFuelType = VehicleFuelOptions.compact(fuelType)
        if (compactFuelType.isBlank()) return VehicleFuelUpdateResult.Failure

        return try {
            vehicleDao.insertIfAbsent(
                VehicleEntity(
                    id = Rifornimento.DEFAULT_VEHICLE_ID,
                    name = "Veicolo",
                    defaultFuelType = MimitStationPriceSelector.FALLBACK_FUEL_TYPE,
                    tankCapacityMilliliters = null,
                ),
            )
            updateResult(
                updatedRows = vehicleDao.updateDefaultFuelType(
                    vehicleId = Rifornimento.DEFAULT_VEHICLE_ID,
                    fuelType = compactFuelType,
                ),
                fuelType = compactFuelType,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            VehicleFuelUpdateResult.Failure
        }
    }

    internal fun updateResult(
        updatedRows: Int,
        fuelType: String,
    ): VehicleFuelUpdateResult = if (updatedRows == 1) {
        VehicleFuelUpdateResult.Success(fuelType)
    } else {
        VehicleFuelUpdateResult.Failure
    }
}
