package com.archimede.w2full.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.local.MimitSyncStateEntity
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.data.mimit.EniStationDistanceService
import com.archimede.w2full.data.mimit.MimitDataSource
import com.archimede.w2full.data.mimit.MimitDataset
import com.archimede.w2full.data.mimit.MimitLogger
import com.archimede.w2full.data.mimit.MimitPrice
import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.RoomNearbyStationsRepository
import com.archimede.w2full.domain.model.RifornimentoDraft
import com.archimede.w2full.location.UserLocationProvider
import com.archimede.w2full.location.UserLocationResult
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VehicleSettingsRepositoryTest {
    private lateinit var database: W2FullDatabase
    private lateinit var repository: RoomVehicleSettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomVehicleSettingsRepository(
            vehicleDao = database.vehicleDao(),
            cacheDao = database.mimitCacheDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `base fuel options are available without vehicle or MIMIT cache`() = runBlocking {
        val settings = repository.observeSettings().first()

        assertEquals("Veicolo", settings.vehicleName)
        assertEquals("Benzina", settings.selectedFuelType)
        assertEquals(listOf("Benzina", "Gasolio", "GPL", "Metano"), settings.fuelOptions)
    }

    @Test
    fun `cached fuel descriptions are added and deduplicated by normalized value`() = runBlocking {
        insertStationAndPrices(
            MimitPriceEntity(1, " gasolio ", 1_700, true, "2026-09-01T08:00:00"),
            MimitPriceEntity(1, "GASOLIO", 1_710, false, "2026-09-01T09:00:00"),
            MimitPriceEntity(1, "HVO", 1_800, true, "2026-09-01T08:00:00"),
        )

        val settings = repository.observeSettings().first()

        assertEquals(1, settings.fuelOptions.count { VehicleFuelOptions.normalize(it) == "gasolio" })
        assertTrue(settings.fuelOptions.any { it == "HVO" })
    }

    @Test
    fun `current value stays selectable even when it is not in cache or base list`() = runBlocking {
        database.vehicleDao().insertIfAbsent(
            VehicleEntity(
                id = 1,
                name = "Auto",
                defaultFuelType = "E-Fuel",
                tankCapacityMilliliters = 50_000,
            ),
        )

        val settings = repository.observeSettings().first()

        assertEquals("E-Fuel", settings.selectedFuelType)
        assertTrue(settings.fuelOptions.contains("E-Fuel"))
    }

    @Test
    fun `fuel selection is persisted without changing refueling records`() = runBlocking {
        val refuelingRepository = RefuelingRepository(database.vehicleDao(), database.rifornimentoDao())
        refuelingRepository.insert(
            RifornimentoDraft(
                timestampEpochMillis = 1_000,
                odometerKm = 10_000,
                litersMilliliters = 40_000,
                totalCostCents = 7_000,
                fuelType = "Benzina",
                isFullTank = true,
            ),
        )
        val before = refuelingRepository.observeRefuelings().first()

        val result = repository.setDefaultFuelType("  Gasolio ")

        assertEquals(VehicleFuelUpdateResult.Success("Gasolio"), result)
        assertEquals("Gasolio", database.vehicleDao().getById(1)?.defaultFuelType)
        assertEquals(before, refuelingRepository.observeRefuelings().first())
    }

    @Test
    fun `zero updated rows maps to failure without throwing`() {
        assertEquals(
            VehicleFuelUpdateResult.Failure,
            repository.updateResult(updatedRows = 0, fuelType = "Gasolio"),
        )
    }

    @Test
    fun `changing vehicle fuel changes cached station prices without refresh`() = runBlocking {
        database.vehicleDao().insertIfAbsent(
            VehicleEntity(
                id = 1,
                name = "Auto",
                defaultFuelType = "Benzina",
                tankCapacityMilliliters = null,
            ),
        )
        insertStationAndPrices(
            MimitPriceEntity(1, "Benzina", 2_059, true, "2026-09-01T08:00:00"),
            MimitPriceEntity(1, "Gasolio", 1_899, true, "2026-09-01T08:00:00"),
        )
        database.mimitCacheDao().upsertSyncState(
            MimitSyncStateEntity(
                stationsExtractionEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(),
                pricesExtractionEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(),
                lastSuccessfulUpdateEpochMillis = 1_000,
            ),
        )

        val nearbyRepository = RoomNearbyStationsRepository(
            database = database,
            cacheDao = database.mimitCacheDao(),
            vehicleDao = database.vehicleDao(),
            dataSource = NeverRefreshDataSource,
            distanceService = EniStationDistanceService(
                userLocationProvider = object : UserLocationProvider {
                    override suspend fun currentLocation(): UserLocationResult = UserLocationResult.Unavailable
                },
            ),
            logger = MimitLogger { _, _ -> },
        )

        val before = requireNotNull(nearbyRepository.loadCachedSnapshot())
        assertEquals("Benzina", before.selectedFuelType)
        assertEquals(2_059L, before.pricesByStationId.getValue(1).self?.priceMilliEuroPerUnit)

        assertEquals(
            VehicleFuelUpdateResult.Success("Gasolio"),
            repository.setDefaultFuelType("Gasolio"),
        )

        val after = requireNotNull(nearbyRepository.loadCachedSnapshot())
        assertEquals("Gasolio", after.selectedFuelType)
        assertEquals(1_899L, after.pricesByStationId.getValue(1).self?.priceMilliEuroPerUnit)
    }

    private suspend fun insertStationAndPrices(vararg prices: MimitPriceEntity) {
        database.mimitCacheDao().insertStations(
            listOf(
                MimitStationEntity(
                    stationId = 1,
                    manager = "Gestore",
                    brand = "Agip Eni",
                    stationType = "Stradale",
                    name = "Eni Test",
                    address = "Via Test 1",
                    municipality = "Modena",
                    province = "MO",
                    latitude = null,
                    longitude = null,
                ),
            ),
        )
        database.mimitCacheDao().insertPrices(prices.toList())
    }

    private object NeverRefreshDataSource : MimitDataSource {
        override fun downloadStations(): MimitDataset<MimitStation> = error("Network refresh must not be called")

        override fun downloadPrices(): MimitDataset<MimitPrice> = error("Network refresh must not be called")
    }
}
