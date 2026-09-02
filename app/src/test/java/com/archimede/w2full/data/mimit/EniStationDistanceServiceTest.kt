package com.archimede.w2full.data.mimit

import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.UserLocationProvider
import com.archimede.w2full.location.UserLocationResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EniStationDistanceServiceTest {
    @Test
    fun availableLocationFiltersEniAndOrdersByDistance() = runBlocking {
        val stations = listOf(
            station(id = 1, brand = "Eni", name = "Roma", latitude = 41.9028, longitude = 12.4964),
            station(id = 2, brand = "Q8", name = "Q8 Roma", latitude = 41.9028, longitude = 12.4964),
            station(id = 3, brand = " ENI ", name = "Milano", latitude = 45.4642, longitude = 9.1900),
            station(id = 4, brand = "eni", name = "Senza coordinate", latitude = null, longitude = null),
        )
        val service = EniStationDistanceService(
            FakeLocationProvider(UserLocationResult.Available(GeoPoint(41.9028, 12.4964))),
        )

        val result = service.rank(stations)

        assertEquals(listOf(1L, 3L, 4L), result.stations.map { it.station.id })
        assertEquals(0.0, result.stations[0].distanceKm!!, 1e-9)
        assertEquals(476.885, result.stations[1].distanceKm!!, 0.5)
        assertNull(result.stations[2].distanceKm)
    }

    @Test
    fun invalidStationCoordinatesAreTreatedAsUnavailableAndSortedLast() = runBlocking {
        val stations = listOf(
            station(id = 1, brand = "Eni", name = "Valida", latitude = 41.9, longitude = 12.5),
            station(id = 2, brand = "Eni", name = "Invalida", latitude = 999.0, longitude = 12.5),
        )
        val service = EniStationDistanceService(
            FakeLocationProvider(UserLocationResult.Available(GeoPoint(41.9, 12.5))),
        )

        val result = service.rank(stations)

        assertEquals(listOf(1L, 2L), result.stations.map { it.station.id })
        assertNull(result.stations[1].distanceKm)
    }

    @Test
    fun permissionDeniedUsesAlphabeticalFallbackWithoutDistances() = runBlocking {
        val service = EniStationDistanceService(FakeLocationProvider(UserLocationResult.PermissionDenied))
        val stations = listOf(
            station(id = 1, brand = "Eni", name = "Zulu", latitude = 41.0, longitude = 12.0),
            station(id = 2, brand = "Eni", name = "Alpha", latitude = 42.0, longitude = 13.0),
            station(id = 3, brand = "Q8", name = "Aardvark", latitude = 40.0, longitude = 11.0),
        )

        val result = service.rank(stations)

        assertSame(UserLocationResult.PermissionDenied, result.locationResult)
        assertEquals(listOf("Alpha", "Zulu"), result.stations.map { it.station.name })
        assertTrue(result.stations.all { it.distanceKm == null })
    }

    @Test
    fun unavailableLocationUsesNameFallbackAddressAndMunicipality() = runBlocking {
        val service = EniStationDistanceService(FakeLocationProvider(UserLocationResult.Unavailable))
        val stations = listOf(
            station(id = 1, brand = "Eni", name = "Zulu", address = "Via Z", municipality = "Roma"),
            station(id = 2, brand = "Eni", name = "", address = "Beta", municipality = "Milano"),
            station(id = 3, brand = "Eni", name = "Alpha", address = "Via A", municipality = "Torino"),
        )

        val result = service.rank(stations)

        assertSame(UserLocationResult.Unavailable, result.locationResult)
        assertEquals(listOf(3L, 2L, 1L), result.stations.map { it.station.id })
        assertTrue(result.stations.all { it.distanceKm == null })
    }

    @Test
    fun providerFailureDegradesToUnavailableInsteadOfCrashing() = runBlocking {
        val service = EniStationDistanceService(
            object : UserLocationProvider {
                override suspend fun currentLocation(): UserLocationResult {
                    error("location provider unavailable")
                }
            },
        )

        val result = service.rank(
            listOf(
                station(id = 1, brand = "Eni", name = "Bravo"),
                station(id = 2, brand = "Eni", name = "Alpha"),
            ),
        )

        assertSame(UserLocationResult.Unavailable, result.locationResult)
        assertEquals(listOf("Alpha", "Bravo"), result.stations.map { it.station.name })
    }

    @Test
    fun cancellationIsPropagated() {
        val service = EniStationDistanceService(
            object : UserLocationProvider {
                override suspend fun currentLocation(): UserLocationResult {
                    throw CancellationException("cancelled")
                }
            },
        )

        assertThrows(CancellationException::class.java) {
            runBlocking { service.rank(listOf(station(id = 1, brand = "Eni", name = "Alpha"))) }
        }
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
        address: String = "Via Roma",
        municipality: String = "Roma",
        latitude: Double? = null,
        longitude: Double? = null,
    ): MimitStation = MimitStation(
        id = id,
        manager = "Gestore",
        brand = brand,
        stationType = "Stradale",
        name = name,
        address = address,
        municipality = municipality,
        province = "RM",
        latitude = latitude,
        longitude = longitude,
    )
}
