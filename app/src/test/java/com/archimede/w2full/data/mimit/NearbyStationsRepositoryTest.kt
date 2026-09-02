package com.archimede.w2full.data.mimit

import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.UserLocationProvider
import com.archimede.w2full.location.UserLocationResult
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyStationsRepositoryTest {
    @Test
    fun sessionRepositoryDownloadsInMemoryAndRanksEniByDistance() = runBlocking {
        val dataset = MimitDataset(
            extractionDate = LocalDate.of(2026, 9, 2),
            rows = listOf(
                station(id = 1, brand = "Eni", name = "Roma", latitude = 41.9028, longitude = 12.4964),
                station(id = 2, brand = "Q8", name = "Q8 Roma", latitude = 41.9028, longitude = 12.4964),
                station(id = 3, brand = "eni", name = "Milano", latitude = 45.4642, longitude = 9.1900),
            ),
        )
        val repository = SessionNearbyStationsRepository(
            stationSource = MimitStationsDataSource { dataset },
            distanceService = EniStationDistanceService(
                FakeLocationProvider(UserLocationResult.Available(GeoPoint(41.9028, 12.4964))),
            ),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val snapshot = repository.loadStations()

        assertEquals(LocalDate.of(2026, 9, 2), snapshot.extractionDate)
        assertEquals(listOf(1L, 3L), snapshot.rankedStations.stations.map { it.station.id })
        assertEquals(0.0, snapshot.rankedStations.stations[0].distanceKm!!, 1e-9)
        assertEquals(476.885, snapshot.rankedStations.stations[1].distanceKm!!, 0.5)
    }

    @Test
    fun deniedPermissionStillReturnsAlphabeticallySortedEniStations() = runBlocking {
        val dataset = MimitDataset(
            extractionDate = LocalDate.of(2026, 9, 2),
            rows = listOf(
                station(id = 1, brand = "Eni", name = "Zulu", latitude = 41.0, longitude = 12.0),
                station(id = 2, brand = "Eni", name = "Alpha", latitude = 42.0, longitude = 13.0),
                station(id = 3, brand = "Q8", name = "Aardvark", latitude = 40.0, longitude = 11.0),
            ),
        )
        val repository = SessionNearbyStationsRepository(
            stationSource = MimitStationsDataSource { dataset },
            distanceService = EniStationDistanceService(
                FakeLocationProvider(UserLocationResult.PermissionDenied),
            ),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val snapshot = repository.loadStations()

        assertSame(UserLocationResult.PermissionDenied, snapshot.rankedStations.locationResult)
        assertEquals(listOf("Alpha", "Zulu"), snapshot.rankedStations.stations.map { it.station.name })
        assertTrue(snapshot.rankedStations.stations.all { it.distanceKm == null })
        assertNull(snapshot.rankedStations.stations.first().distanceKm)
    }

    private class FakeLocationProvider(
        private val result: UserLocationResult,
    ) : UserLocationProvider {
        override suspend fun currentLocation(): UserLocationResult = result
    }

    private fun station(
        id: Long,
        brand: String,
        name: String,
        latitude: Double?,
        longitude: Double?,
    ): MimitStation = MimitStation(
        id = id,
        manager = "Gestore",
        brand = brand,
        stationType = "Stradale",
        name = name,
        address = "Via Roma",
        municipality = "Roma",
        province = "RM",
        latitude = latitude,
        longitude = longitude,
    )
}
