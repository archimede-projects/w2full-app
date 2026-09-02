package com.archimede.w2full.data.mimit

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.local.MimitSyncStateEntity
import com.archimede.w2full.data.local.VehicleEntity
import com.archimede.w2full.data.local.W2FullDatabase
import com.archimede.w2full.location.UserLocationProvider
import com.archimede.w2full.location.UserLocationResult
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NearbyStationsRepositoryTest {
    private lateinit var database: W2FullDatabase
    private lateinit var server: MockWebServer
    private lateinit var logger: RecordingMimitLogger

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, W2FullDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        server = MockWebServer()
        server.start()
        logger = RecordingMimitLogger()
    }

    @After
    fun tearDown() {
        server.close()
        database.close()
    }

    @Test
    fun validCsvAtomicallyReplacesCacheAndUpdatesTimestamp() = runBlocking {
        seedOldCache()
        enqueueValidDatasets()
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result is MimitRefreshResult.Success)
        assertEquals(NEW_TIMESTAMP, (result as MimitRefreshResult.Success).lastSuccessfulUpdateEpochMillis)
        assertEquals(listOf(12345L), database.mimitCacheDao().getStations().map { it.stationId })
        assertEquals(listOf(12345L, 12345L), database.mimitCacheDao().getPrices().map { it.stationId })
        val state = requireNotNull(database.mimitCacheDao().getSyncState())
        assertEquals(NEW_TIMESTAMP, state.lastSuccessfulUpdateEpochMillis)
        assertEquals(LocalDate.of(2026, 9, 1).toEpochDay(), state.stationsExtractionEpochDay)
        assertEquals(LocalDate.of(2026, 9, 1).toEpochDay(), state.pricesExtractionEpochDay)
        assertTrue(logger.entries.isEmpty())
    }

    @Test
    fun liveAgipEniBrandRefreshesInsteadOfProducingEmptyEniSet() = runBlocking {
        server.enqueue(
            csvResponse(
                """
                Estrazione del 2026-09-01
                idImpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
                555|Gestore|Agip Eni|Stradale|Eni Test|Via Test|Roma|RM|41.9|12.5
                """.trimIndent(),
            ),
        )
        server.enqueue(
            csvResponse(
                """
                Estrazione del 2026-09-01
                idImpianto|descCarburante|prezzo|isSelf|dtComu
                555|Benzina|1.789|1|01/09/2026 08:00:00
                """.trimIndent(),
            ),
        )
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result is MimitRefreshResult.Success)
        assertEquals(listOf(555L), database.mimitCacheDao().getStations().map { it.stationId })
        assertEquals("Agip Eni", database.mimitCacheDao().getStations().single().brand)
    }

    @Test
    fun cachedSnapshotUsesVehicleDefaultFuelAndNewestPrices() = runBlocking {
        seedOldCache()
        database.vehicleDao().insertIfAbsent(
            VehicleEntity(
                id = 1,
                name = "Veicolo",
                defaultFuelType = "  GASOLIO ",
                tankCapacityMilliliters = null,
            ),
        )
        database.mimitCacheDao().insertPrices(
            listOf(
                MimitPriceEntity(999, "Gasolio", 1_600, true, "2026-09-01T08:00:00"),
                MimitPriceEntity(999, "gasolio", 1_650, true, "2026-09-02T08:00:00"),
                MimitPriceEntity(999, "Gasolio", 1_799, false, "2026-09-02T09:00:00"),
                MimitPriceEntity(999, "Gasolio Plus", 1_100, false, "2026-09-02T10:00:00"),
            ),
        )

        val snapshot = requireNotNull(repository().loadCachedSnapshot())
        val selected = requireNotNull(snapshot.pricesByStationId[999])

        assertEquals("GASOLIO", snapshot.selectedFuelType)
        assertEquals(1_650L, selected.self?.priceMilliEuroPerUnit)
        assertEquals(1_799L, selected.served?.priceMilliEuroPerUnit)
        assertEquals(MimitPriceUnit.LITER, selected.unit)
    }

    @Test
    fun missingVehicleFallsBackToBenzinaAndMissingCompatibleFuelStaysUnavailable() = runBlocking {
        seedOldCache()
        val fallback = requireNotNull(repository().loadCachedSnapshot())

        assertEquals(MimitStationPriceSelector.FALLBACK_FUEL_TYPE, fallback.selectedFuelType)
        assertEquals(1_700L, fallback.pricesByStationId[999]?.self?.priceMilliEuroPerUnit)

        database.mimitCacheDao().clearPrices()
        database.mimitCacheDao().insertPrices(
            listOf(MimitPriceEntity(999, "Gasolio", 1_600, true, "2026-09-02T08:00:00")),
        )
        val withoutCompatiblePrice = requireNotNull(repository().loadCachedSnapshot())
        assertNull(withoutCompatiblePrice.pricesByStationId[999])
    }

    @Test
    fun changedHeaderIsInterceptedAndOldCacheAndTimestampStayUntouched() = runBlocking {
        seedOldCache()
        server.enqueue(
            csvResponse(
                """
                Estrazione del 2026-09-02
                idimpianto|Gestore|Bandiera|COLONNA CAMBIATA|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
                1|Gestore|Eni|Stradale|Uno|Via Uno|Roma|RM|41.9|12.5
                """.trimIndent(),
            ),
        )
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result is MimitRefreshResult.Failure)
        assertFalse((result as MimitRefreshResult.Failure).retryable)
        assertOldCacheUntouched()
        assertTrue(logger.entries.single().message.contains("Expected header not found"))
        assertTrue(logger.entries.single().throwable is MimitCsvFormatException)
    }

    @Test
    fun priceParseFailureAfterStationDownloadLeavesOldCacheUntouched() = runBlocking {
        seedOldCache()
        server.enqueue(csvResponse(resourceText("mimit/anagrafica_sample.csv")))
        server.enqueue(
            csvResponse(
                """
                Estrazione del 2026-09-02
                idimpianto|descCarburante|prezzo|isSelf|dtComu
                12345|Benzina|not-a-price|1|02/09/2026 08:00:00
                """.trimIndent(),
            ),
        )
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result is MimitRefreshResult.Failure)
        assertFalse((result as MimitRefreshResult.Failure).retryable)
        assertOldCacheUntouched()
        val cached = repository.loadCachedSnapshot()
        assertNotNull(cached)
        assertEquals(listOf(999L), cached!!.rankedStations.stations.map { it.station.id })
        assertEquals(OLD_TIMESTAMP, cached.lastSuccessfulUpdateEpochMillis)
    }

    @Test
    fun downloadFailureIsRetryableAndLeavesOldCacheAndTimestampUntouched() = runBlocking {
        seedOldCache()
        server.enqueue(
            MockResponse.Builder()
                .code(503)
                .body("temporarily unavailable")
                .build(),
        )
        val repository = repository()

        val result = repository.refresh()

        assertTrue(result is MimitRefreshResult.Failure)
        assertTrue((result as MimitRefreshResult.Failure).retryable)
        assertOldCacheUntouched()
        assertTrue(logger.entries.single().message.contains("HTTP 503"))
    }

    @Test
    fun locationCanResolveAfterRefreshFailureWithNoCache() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(503)
                .body("temporarily unavailable")
                .build(),
        )
        val repository = repository(locationResult = UserLocationResult.PermissionDenied)

        val refreshResult = repository.refresh()
        val locationResult = repository.resolveLocation()

        assertTrue(refreshResult is MimitRefreshResult.Failure)
        assertSame(UserLocationResult.PermissionDenied, locationResult)
        assertTrue(database.mimitCacheDao().getStations().isEmpty())
    }

    private fun repository(
        locationResult: UserLocationResult = UserLocationResult.PermissionDenied,
    ): RoomNearbyStationsRepository {
        val client = MimitCsvClient(
            httpClient = OkHttpClient(),
            stationsUrl = server.url("/stations.csv").toString(),
            pricesUrl = server.url("/prices.csv").toString(),
        )
        return RoomNearbyStationsRepository(
            database = database,
            cacheDao = database.mimitCacheDao(),
            vehicleDao = database.vehicleDao(),
            dataSource = client,
            distanceService = EniStationDistanceService(
                object : UserLocationProvider {
                    override suspend fun currentLocation(): UserLocationResult = locationResult
                },
            ),
            logger = logger,
            clock = Clock.fixed(Instant.ofEpochMilli(NEW_TIMESTAMP), ZoneOffset.UTC),
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    private suspend fun seedOldCache() {
        database.withTransaction {
            database.mimitCacheDao().insertStations(
                listOf(
                    MimitStationEntity(
                        stationId = 999,
                        manager = "Old manager",
                        brand = "Eni",
                        stationType = "Stradale",
                        name = "Old cached station",
                        address = "Via Vecchia",
                        municipality = "Roma",
                        province = "RM",
                        latitude = null,
                        longitude = null,
                    ),
                ),
            )
            database.mimitCacheDao().insertPrices(
                listOf(
                    MimitPriceEntity(
                        stationId = 999,
                        fuelDescription = "Benzina",
                        priceMilliEuroPerUnit = 1_700,
                        isSelf = true,
                        communicatedAt = "2026-09-01T08:00:00",
                    ),
                ),
            )
            database.mimitCacheDao().upsertSyncState(
                MimitSyncStateEntity(
                    stationsExtractionEpochDay = LocalDate.of(2026, 8, 31).toEpochDay(),
                    pricesExtractionEpochDay = LocalDate.of(2026, 8, 31).toEpochDay(),
                    lastSuccessfulUpdateEpochMillis = OLD_TIMESTAMP,
                ),
            )
        }
    }

    private suspend fun assertOldCacheUntouched() {
        assertEquals(listOf(999L), database.mimitCacheDao().getStations().map { it.stationId })
        assertEquals(listOf(999L), database.mimitCacheDao().getPrices().map { it.stationId })
        assertEquals(OLD_TIMESTAMP, database.mimitCacheDao().getSyncState()?.lastSuccessfulUpdateEpochMillis)
    }

    private fun enqueueValidDatasets() {
        server.enqueue(csvResponse(resourceText("mimit/anagrafica_sample.csv")))
        server.enqueue(csvResponse(resourceText("mimit/prezzi_sample.csv")))
    }

    private fun csvResponse(body: String): MockResponse = MockResponse.Builder()
        .addHeader("Content-Type", "text/csv; charset=utf-8")
        .body(body)
        .build()

    private fun resourceText(path: String): String = requireNotNull(
        javaClass.classLoader?.getResource(path),
    ) { "Missing test resource $path" }.readText()

    private class RecordingMimitLogger : MimitLogger {
        val entries = mutableListOf<Entry>()

        override fun error(message: String, throwable: Throwable) {
            entries += Entry(message, throwable)
        }

        data class Entry(val message: String, val throwable: Throwable)
    }

    private companion object {
        const val OLD_TIMESTAMP = 1_700_000_000_000L
        val NEW_TIMESTAMP: Long = Instant.parse("2026-09-02T07:00:00Z").toEpochMilli()
    }
}
