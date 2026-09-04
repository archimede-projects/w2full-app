package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitPriceUnit
import com.archimede.w2full.data.mimit.MimitSelectedModePrice
import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.MimitStationFuelPrice
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StationGlobalOrderingM74Test {
    @Test
    fun `favorite state does not pin station ahead of price ordering`() {
        val stations = listOf(
            station(1, 1.0),
            station(2, 2.0),
            station(3, 3.0),
        )
        val prices = mapOf(
            1L to price(self = 2090, served = 2200),
            2L to price(self = 2050, served = 2250),
            3L to price(self = 2070, served = 2100),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = prices,
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(sortMode = StationSortMode.SELF_PRICE),
            favoriteStationIds = setOf(1L, 3L),
        )

        assertEquals(listOf(2L, 3L, 1L), result.map { it.station.id })
    }

    @Test
    fun `favorites scope filters first then keeps selected ordering`() {
        val stations = listOf(
            station(1, 4.0),
            station(2, 1.0),
            station(3, 2.0),
        )
        val prices = mapOf(
            1L to price(self = 2020, served = 2200),
            2L to price(self = 2050, served = 2180),
            3L to price(self = 2030, served = 2150),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = prices,
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(
                sortMode = StationSortMode.SERVED_PRICE,
                scope = StationListScope.FAVORITES,
            ),
            favoriteStationIds = setOf(1L, 3L),
        )

        assertEquals(listOf(3L, 1L), result.map { it.station.id })
    }

    @Test
    fun `distance ordering remains global across favorite and regular stations`() {
        val result = filterAndSortStations(
            stations = listOf(station(1, 5.0), station(2, 1.0), station(3, 3.0)),
            pricesByStationId = emptyMap(),
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(sortMode = StationSortMode.DISTANCE),
            favoriteStationIds = setOf(1L),
        )

        assertEquals(listOf(1L, 2L, 3L), result.map { it.station.id })
    }

    private fun station(id: Long, distanceKm: Double) = MimitStationDistance(
        station = MimitStation(
            id = id,
            manager = "manager",
            brand = "Eni",
            stationType = "stradale",
            name = "Station $id",
            address = "Via $id",
            municipality = "Comune",
            province = "MO",
            latitude = 44.0,
            longitude = 11.0,
        ),
        distanceKm = distanceKm,
    )

    private fun price(self: Long, served: Long) = MimitStationFuelPrice(
        fuelType = "Benzina",
        unit = MimitPriceUnit.LITER,
        self = MimitSelectedModePrice(self, LocalDateTime.of(2026, 9, 4, 12, 0)),
        served = MimitSelectedModePrice(served, LocalDateTime.of(2026, 9, 4, 12, 0)),
    )
}