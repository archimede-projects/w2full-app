package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitPriceUnit
import com.archimede.w2full.data.mimit.MimitSelectedModePrice
import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.MimitStationFuelPrice
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class StationListFiltersTest {
    @Test
    fun noLimitDistanceKeepsRepositoryOrder() {
        val stations = listOf(
            stationDistance(2, 0.8),
            stationDistance(1, 1.8),
            stationDistance(3, null),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = emptyMap(),
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(
                radiusEnabled = false,
                sortMode = StationSortMode.DISTANCE,
            ),
        )

        assertEquals(listOf(2L, 1L, 3L), result.map { it.station.id })
    }

    @Test
    fun enabledRadiusWithLocationExcludesOutsideAndUnknownDistance() {
        val stations = listOf(
            stationDistance(1, 2.0),
            stationDistance(2, 10.0),
            stationDistance(3, 10.1),
            stationDistance(4, null),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = emptyMap(),
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(
                radiusEnabled = true,
                radiusKm = 10,
            ),
        )

        assertEquals(listOf(1L, 2L), result.map { it.station.id })
    }

    @Test
    fun enabledRadiusWithoutLocationDoesNotFilter() {
        val stations = listOf(
            stationDistance(1, null),
            stationDistance(2, null),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = emptyMap(),
            locationStatus = NearbyLocationUiStatus.UNAVAILABLE,
            preferences = StationListPreferences(
                radiusEnabled = true,
                radiusKm = 5,
            ),
        )

        assertEquals(listOf(1L, 2L), result.map { it.station.id })
    }

    @Test
    fun selfPriceSortUsesPriceThenDistanceAndPutsMissingPriceLast() {
        val stations = listOf(
            stationDistance(1, 5.0),
            stationDistance(2, 10.0),
            stationDistance(3, 2.0),
            stationDistance(4, 1.0),
        )
        val prices = mapOf(
            1L to fuelPrice(self = 2_000, served = 2_200),
            2L to fuelPrice(self = 1_900, served = 2_300),
            3L to fuelPrice(self = 1_900, served = 2_100),
            4L to fuelPrice(self = null, served = 2_000),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = prices,
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(sortMode = StationSortMode.SELF_PRICE),
        )

        assertEquals(listOf(3L, 2L, 1L, 4L), result.map { it.station.id })
    }

    @Test
    fun servedPriceSortUsesServedPriceAndDistanceTieBreak() {
        val stations = listOf(
            stationDistance(1, 5.0),
            stationDistance(2, 3.0),
            stationDistance(3, 1.0),
            stationDistance(4, 2.0),
        )
        val prices = mapOf(
            1L to fuelPrice(self = 1_700, served = 2_100),
            2L to fuelPrice(self = 1_600, served = 2_000),
            3L to fuelPrice(self = 1_500, served = null),
            4L to fuelPrice(self = 1_800, served = 2_000),
        )

        val result = filterAndSortStations(
            stations = stations,
            pricesByStationId = prices,
            locationStatus = NearbyLocationUiStatus.AVAILABLE,
            preferences = StationListPreferences(sortMode = StationSortMode.SERVED_PRICE),
        )

        assertEquals(listOf(4L, 2L, 1L, 3L), result.map { it.station.id })
    }

    @Test
    fun radiusValidationAcceptsOnlyOneThroughTwoHundredAndKeepsPreviousOtherwise() {
        assertEquals(1, validatedRadiusOrPrevious("1", 20))
        assertEquals(200, validatedRadiusOrPrevious("200", 20))
        assertEquals(20, validatedRadiusOrPrevious("0", 20))
        assertEquals(20, validatedRadiusOrPrevious("201", 20))
        assertEquals(20, validatedRadiusOrPrevious("abc", 20))
        assertEquals(20, validatedRadiusOrPrevious("", 20))
    }

    private fun stationDistance(id: Long, distanceKm: Double?): MimitStationDistance =
        MimitStationDistance(
            station = MimitStation(
                id = id,
                manager = "manager",
                brand = "Eni",
                stationType = "stradale",
                name = "Eni #$id",
                address = "Via $id",
                municipality = "Comune",
                province = "MO",
                latitude = null,
                longitude = null,
            ),
            distanceKm = distanceKm,
        )

    private fun fuelPrice(self: Long?, served: Long?): MimitStationFuelPrice =
        MimitStationFuelPrice(
            fuelType = "Benzina",
            unit = MimitPriceUnit.LITER,
            self = self?.let(::selectedPrice),
            served = served?.let(::selectedPrice),
        )

    private fun selectedPrice(value: Long): MimitSelectedModePrice = MimitSelectedModePrice(
        priceMilliEuroPerUnit = value,
        communicatedAt = LocalDateTime.of(2026, 9, 4, 12, 0),
    )
}
