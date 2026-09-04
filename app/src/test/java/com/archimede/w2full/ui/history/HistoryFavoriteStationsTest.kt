package com.archimede.w2full.ui.history

import com.archimede.w2full.data.repository.PriceHistoryStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryFavoriteStationsTest {
    private val stations = listOf(
        station(1, "Uno"),
        station(2, "Due"),
        station(3, "Tre"),
    )

    @Test
    fun groupsFavoritesWithoutDroppingOtherStations() {
        val groups = groupHistoryStations(stations, setOf(1, 3))

        assertEquals(listOf(1L, 3L), groups.favorites.map { it.stationId })
        assertEquals(listOf(2L), groups.others.map { it.stationId })
        assertEquals(
            stations.map { it.stationId }.toSet(),
            (groups.favorites + groups.others).map { it.stationId }.toSet(),
        )
    }

    @Test
    fun historyListContainsOnlyFavoriteStationsWithHistory() {
        assertEquals(
            listOf(1L, 3L),
            historyStationsForFavorites(stations, setOf(1, 3, 999)).map { it.stationId },
        )
    }

    @Test
    fun historyListIsEmptyWhenNoFavoriteHasHistory() {
        assertTrue(historyStationsForFavorites(stations, setOf(999)).isEmpty())
    }

    @Test
    fun selectionPrefersFavoriteWhenCurrentSelectionIsInvalid() {
        assertEquals(
            3L,
            resolveHistoryStationSelection(
                currentStationId = 999,
                stations = stations,
                favoriteStationIds = setOf(3),
            ),
        )
    }

    @Test
    fun selectionKeepsCurrentValidStationEvenWhenItIsNotFavorite() {
        assertEquals(
            2L,
            resolveHistoryStationSelection(
                currentStationId = 2,
                stations = stations,
                favoriteStationIds = setOf(3),
            ),
        )
    }

    @Test
    fun filteredHistorySelectionDropsRemovedFavoriteAndFallsBack() {
        val favoriteStations = historyStationsForFavorites(stations, setOf(1, 3))
        assertEquals(
            1L,
            resolveHistoryStationSelection(
                currentStationId = 2,
                stations = favoriteStations,
                favoriteStationIds = setOf(1, 3),
            ),
        )

        val noFavorites = historyStationsForFavorites(stations, emptySet())
        assertNull(
            resolveHistoryStationSelection(
                currentStationId = 1,
                stations = noFavorites,
                favoriteStationIds = emptySet(),
            ),
        )
    }

    @Test
    fun togglingSelectedStationKeepsSelectionResolvable() {
        val selected = 2L
        val favorites = toggledHistoryFavoriteStationIds(emptySet(), selected)

        assertTrue(selected in favorites)
        assertEquals(
            selected,
            resolveHistoryStationSelection(selected, stations, favorites),
        )

        val removed = toggledHistoryFavoriteStationIds(favorites, selected)
        assertFalse(selected in removed)
        assertEquals(
            selected,
            resolveHistoryStationSelection(selected, stations, removed),
        )
    }

    private fun station(id: Long, name: String) = PriceHistoryStation(
        stationId = id,
        name = name,
        address = "",
        municipality = "",
        province = "",
    )
}
