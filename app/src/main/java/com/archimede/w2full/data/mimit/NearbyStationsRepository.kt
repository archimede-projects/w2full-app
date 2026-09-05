package com.archimede.w2full.data.mimit

import androidx.room.withTransaction
import com.archimede.w2full.data.local.MimitCacheDao
import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitPriceHistoryEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.local.MimitSyncStateEntity
import com.archimede.w2full.data.local.VehicleDao
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.domain.model.Rifornimento
import com.archimede.w2full.location.UserLocationResult
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class NearbyStationsSnapshot(
    val extractionDate: LocalDate,
    val pricesExtractionDate: LocalDate,
    val rankedStations: RankedEniStations,
    val lastSuccessfulUpdateEpochMillis: Long,
    val selectedFuelType: String = MimitStationPriceSelector.FALLBACK_FUEL_TYPE,
    val pricesByStationId: Map<Long, MimitStationFuelPrice> = emptyMap(),
)

sealed interface MimitRefreshResult {
    data class Success(val lastSuccessfulUpdateEpochMillis: Long) : MimitRefreshResult
    data class Failure(val retryable: Boolean) : MimitRefreshResult
}

interface NearbyStationsRepository {
    fun observeStations(): Flow<NearbyStationsSnapshot?>

    suspend fun loadCachedSnapshot(): NearbyStationsSnapshot?

    suspend fun resolveLocation(): UserLocationResult

    suspend fun refresh(): MimitRefreshResult
}

class MimitCacheValidationException(message: String) : IllegalArgumentException(message)

class RoomNearbyStationsRepository(
    private val database: W2FullDatabase,
    private val cacheDao: MimitCacheDao,
    private val vehicleDao: VehicleDao,
    private val dataSource: MimitDataSource,
    private val distanceService: EniStationDistanceService,
    private val logger: MimitLogger,
    private val clock: Clock = Clock.systemUTC(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : NearbyStationsRepository {
    override fun observeStations(): Flow<NearbyStationsSnapshot?> =
        combine(
            cacheDao.observeStations(),
            cacheDao.observePrices(),
            cacheDao.observeSyncState(),
            vehicleDao.observeById(Rifornimento.DEFAULT_VEHICLE_ID),
        ) { stations, prices, syncState, vehicle ->
            SnapshotInput(stations, prices, syncState, vehicle)
        }
            .map { input ->
                toSnapshot(
                    stationEntities = input.stations,
                    priceEntities = input.prices,
                    syncState = input.syncState,
                    vehicle = input.vehicle,
                )
            }
            .flowOn(ioDispatcher)

    override suspend fun loadCachedSnapshot(): NearbyStationsSnapshot? = withContext(ioDispatcher) {
        toSnapshot(
            stationEntities = cacheDao.getStations(),
            priceEntities = cacheDao.getPrices(),
            syncState = cacheDao.getSyncState(),
            vehicle = vehicleDao.getById(Rifornimento.DEFAULT_VEHICLE_ID),
        )
    }

    override suspend fun resolveLocation(): UserLocationResult = withContext(ioDispatcher) {
        distanceService.resolveLocation()
    }

    override suspend fun refresh(): MimitRefreshResult = withContext(ioDispatcher) {
        try {
            val stationDataset = dataSource.downloadStations()
            val eniStations = MimitStationFilter.eniStations(stationDataset.rows)
            val priceDataset = dataSource.downloadPrices()
            val eniStationIds = eniStations.mapTo(linkedSetOf()) { it.id }
            val eniPrices = priceDataset.rows.filter { it.stationId in eniStationIds }

            validate(
                stationRows = stationDataset.rows,
                priceRows = priceDataset.rows,
                eniStations = eniStations,
                eniPrices = eniPrices,
            )

            val stationEntities = eniStations.map { it.toEntity() }
            val priceEntities = eniPrices.map { it.toEntity() }
            val successfulAt = clock.millis()
            val observedOnEpochDay = priceDataset.extractionDate.toEpochDay()
            val historyEntities = eniPrices.map {
                it.toHistoryEntity(
                    observedOnEpochDay = observedOnEpochDay,
                    importedAtEpochMillis = successfulAt,
                )
            }
            val syncState = MimitSyncStateEntity(
                stationsExtractionEpochDay = stationDataset.extractionDate.toEpochDay(),
                pricesExtractionEpochDay = observedOnEpochDay,
                lastSuccessfulUpdateEpochMillis = successfulAt,
            )

            database.withTransaction {
                cacheDao.insertPriceHistory(historyEntities)
                cacheDao.clearPrices()
                cacheDao.clearStations()
                cacheDao.clearSyncState()
                cacheDao.insertStations(stationEntities)
                cacheDao.insertPrices(priceEntities)
                cacheDao.upsertSyncState(syncState)
            }

            MimitRefreshResult.Success(successfulAt)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            logger.error(
                "MIMIT refresh failed: ${exception::class.java.simpleName}: ${exception.message.orEmpty()}",
                exception,
            )
            MimitRefreshResult.Failure(retryable = exception is IOException)
        }
    }

    private suspend fun toSnapshot(
        stationEntities: List<MimitStationEntity>,
        priceEntities: List<MimitPriceEntity>,
        syncState: MimitSyncStateEntity?,
        vehicle: VehicleEntity?,
    ): NearbyStationsSnapshot? {
        if (stationEntities.isEmpty() || syncState == null) return null
        val stations = stationEntities.map { it.toModel() }
        val priceSelection = MimitStationPriceSelector.select(
            prices = priceEntities.map { it.toModel() },
            defaultFuelType = vehicle?.defaultFuelType,
        )
        return NearbyStationsSnapshot(
            extractionDate = LocalDate.ofEpochDay(syncState.stationsExtractionEpochDay),
            pricesExtractionDate = LocalDate.ofEpochDay(syncState.pricesExtractionEpochDay),
            rankedStations = distanceService.rank(stations),
            lastSuccessfulUpdateEpochMillis = syncState.lastSuccessfulUpdateEpochMillis,
            selectedFuelType = priceSelection.fuelType,
            pricesByStationId = priceSelection.pricesByStationId,
        )
    }

    private fun validate(
        stationRows: List<MimitStation>,
        priceRows: List<MimitPrice>,
        eniStations: List<MimitStation>,
        eniPrices: List<MimitPrice>,
    ) {
        if (stationRows.isEmpty()) {
            throw MimitCacheValidationException("Station dataset is empty")
        }
        if (priceRows.isEmpty()) {
            throw MimitCacheValidationException("Price dataset is empty")
        }
        if (eniStations.isEmpty()) {
            throw MimitCacheValidationException("No Eni stations found in station dataset")
        }
        if (eniPrices.isEmpty()) {
            throw MimitCacheValidationException("No prices found for cached Eni stations")
        }
        if (eniStations.map { it.id }.toSet().size != eniStations.size) {
            throw MimitCacheValidationException("Duplicate Eni station IDs in station dataset")
        }
        val priceKeys = eniPrices.map {
            PriceKey(it.stationId, it.fuelDescription, it.isSelf, it.communicatedAt.toString())
        }
        if (priceKeys.toSet().size != priceKeys.size) {
            throw MimitCacheValidationException("Duplicate Eni price rows in price dataset")
        }
    }

    private data class SnapshotInput(
        val stations: List<MimitStationEntity>,
        val prices: List<MimitPriceEntity>,
        val syncState: MimitSyncStateEntity?,
        val vehicle: VehicleEntity?,
    )

    private data class PriceKey(
        val stationId: Long,
        val fuelDescription: String,
        val isSelf: Boolean,
        val communicatedAt: String,
    )

    private fun MimitStation.toEntity(): MimitStationEntity = MimitStationEntity(
        stationId = id,
        manager = manager,
        brand = brand,
        stationType = stationType,
        name = name,
        address = address,
        municipality = municipality,
        province = province,
        latitude = latitude,
        longitude = longitude,
    )

    private fun MimitStationEntity.toModel(): MimitStation = MimitStation(
        id = stationId,
        manager = manager,
        brand = brand,
        stationType = stationType,
        name = name,
        address = address,
        municipality = municipality,
        province = province,
        latitude = latitude,
        longitude = longitude,
    )

    private fun MimitPrice.toEntity(): MimitPriceEntity = MimitPriceEntity(
        stationId = stationId,
        fuelDescription = fuelDescription,
        priceMilliEuroPerUnit = priceMilliEuroPerUnit,
        isSelf = isSelf,
        communicatedAt = communicatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    )

    private fun MimitPrice.toHistoryEntity(
        observedOnEpochDay: Long,
        importedAtEpochMillis: Long,
    ): MimitPriceHistoryEntity = MimitPriceHistoryEntity(
        stationId = stationId,
        fuelDescription = fuelDescription,
        priceMilliEuroPerUnit = priceMilliEuroPerUnit,
        isSelf = isSelf,
        observedOnEpochDay = observedOnEpochDay,
        communicatedAt = communicatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        importedAtEpochMillis = importedAtEpochMillis,
    )

    private fun MimitPriceEntity.toModel(): MimitPrice = MimitPrice(
        stationId = stationId,
        fuelDescription = fuelDescription,
        priceMilliEuroPerUnit = priceMilliEuroPerUnit,
        isSelf = isSelf,
        communicatedAt = LocalDateTime.parse(communicatedAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    )
}
