package com.archimede.w2full.data.repository

import com.archimede.w2full.data.local.MimitCacheDao
import com.archimede.w2full.data.local.MimitPriceHistoryEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.local.VehicleDao
import com.archimede.w2full.domain.model.Rifornimento
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class PriceHistoryStation(
    val stationId: Long,
    val name: String,
    val address: String,
    val municipality: String,
    val province: String,
)

data class PriceHistoryPoint(
    val communicatedAt: LocalDateTime,
    val priceMilliEuroPerUnit: Long,
    val importedAtEpochMillis: Long,
) {
    val priceEuroPerUnit: Double
        get() = priceMilliEuroPerUnit / 1_000.0
}

interface PriceHistoryRepository {
    fun observeStationsWithHistory(): Flow<List<PriceHistoryStation>>

    fun observeFuelTypes(stationId: Long): Flow<List<String>>

    fun observeServiceModes(stationId: Long, fuelDescription: String): Flow<List<Boolean>>

    fun observeSeries(
        stationId: Long,
        fuelDescription: String,
        isSelf: Boolean,
    ): Flow<List<PriceHistoryPoint>>

    fun observeDefaultFuelType(): Flow<String?>
}

class RoomPriceHistoryRepository(
    private val cacheDao: MimitCacheDao,
    private val vehicleDao: VehicleDao,
) : PriceHistoryRepository {
    override fun observeStationsWithHistory(): Flow<List<PriceHistoryStation>> =
        cacheDao.observeStationsWithHistory().map { stations -> stations.map { it.toHistoryStation() } }

    override fun observeFuelTypes(stationId: Long): Flow<List<String>> =
        cacheDao.observeHistoryFuelDescriptions(stationId)

    override fun observeServiceModes(
        stationId: Long,
        fuelDescription: String,
    ): Flow<List<Boolean>> = cacheDao.observeHistoryServiceModes(stationId, fuelDescription)

    override fun observeSeries(
        stationId: Long,
        fuelDescription: String,
        isSelf: Boolean,
    ): Flow<List<PriceHistoryPoint>> = cacheDao.observePriceHistory(
        stationId = stationId,
        fuelDescription = fuelDescription,
        isSelf = isSelf,
    ).map { history -> history.map { it.toHistoryPoint() } }

    override fun observeDefaultFuelType(): Flow<String?> =
        vehicleDao.observeById(Rifornimento.DEFAULT_VEHICLE_ID).map { it?.defaultFuelType }

    private fun MimitStationEntity.toHistoryStation(): PriceHistoryStation = PriceHistoryStation(
        stationId = stationId,
        name = name,
        address = address,
        municipality = municipality,
        province = province,
    )

    private fun MimitPriceHistoryEntity.toHistoryPoint(): PriceHistoryPoint = PriceHistoryPoint(
        communicatedAt = LocalDateTime.parse(communicatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        priceMilliEuroPerUnit = priceMilliEuroPerUnit,
        importedAtEpochMillis = importedAtEpochMillis,
    )
}
