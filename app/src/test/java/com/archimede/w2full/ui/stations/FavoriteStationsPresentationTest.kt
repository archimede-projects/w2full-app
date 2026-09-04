package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.MimitStationDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteStationsPresentationTest {
    @Test
    fun favoriteOutsideFilteredListRemainsManageableWithoutDuplicates() {
        val near = stationDistance(id = 1, distanceKm = 3.0)
        val farFavorite = stationDistance(id = 2, distanceKm = 120.0)
        val source = listOf(near, farFavorite)
        val filtered = listOf(near)

        val presentation = splitStationsForFavorites(
            sourceStations = source,
            displayedStations = filtered,
            favoriteStationIds = setOf(2),
        )

        assertEquals(listOf(2L), presentation.favorites.map { it.station.id })
        assertEquals(listOf(1L), presentation.regular.map { it.station.id })
    }

    @Test
    fun favoriteInsideFilteredListIsShownOnlyInFavoriteSection() {
        val one = stationDistance(id = 1, distanceKm = 3.0)
        val two = stationDistance(id = 2, distanceKm = 5.0)

        val presentation = splitStationsForFavorites(
            sourceStations = listOf(one, two),
            displayedStations = listOf(one, two),
            favoriteStationIds = setOf(1),
        )

        assertEquals(listOf(1L), presentation.favorites.map { it.station.id })
        assertEquals(listOf(2L), presentation.regular.map { it.station.id })
    }

    @Test
    fun starToggleAddsThenRemovesSameStation() {
        val added = toggledFavoriteStationIds(emptySet(), 56865L)
        assertTrue(56865L in added)

        val removed = toggledFavoriteStationIds(added, 56865L)
        assertFalse(56865L in removed)
        assertTrue(removed.isEmpty())
    }

    private fun stationDistance(id: Long, distanceKm: Double) = MimitStationDistance(
        station = MimitStation(
            id = id,
            manager = "Gestore",
            brand = "Eni",
            stationType = "Stradale",
            name = "Eni #$id",
            address = "Via Test",
            municipality = "Test",
            province = "VI",
            latitude = null,
            longitude = null,
        ),
        distanceKm = distanceKm,
    )
}
