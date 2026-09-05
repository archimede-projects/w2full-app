package com.archimede.w2full.ui.stations

import android.os.Looper
import com.archimede.w2full.data.mimit.EniStationDistanceRanker
import com.archimede.w2full.data.mimit.MimitRefreshResult
import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.NearbyStationsRepository
import com.archimede.w2full.data.mimit.NearbyStationsSnapshot
import com.archimede.w2full.data.mimit.RankedEniStations
import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.LocationLabelResolver
import com.archimede.w2full.location.UserLocationResult
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NearbyStationsLocationRc3Test {
    @Test
    fun retryLocationReranksCachedStationsAppliesRadiusAndResolvesLabel() {
        val repository = CachedStationsLocationRepository()
        val preferences = InMemoryStationListPreferencesStore(
            StationListPreferences(
                radiusEnabled = true,
                radiusKm = 25,
                sortMode = StationSortMode.DISTANCE,
            ),
        )
        val viewModel = NearbyStationsViewModel(
            repository = repository,
            preferencesStore = preferences,
            locationLabelResolver = LocationLabelResolver { "Mirandola (MO)" },
        )

        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(NearbyLocationUiStatus.UNAVAILABLE, viewModel.uiState.value.locationStatus)
        assertEquals(2, viewModel.uiState.value.stations.size)

        viewModel.refreshLocation()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals(1, repository.resolveLocationCalls)
        assertEquals(NearbyLocationUiStatus.AVAILABLE, state.locationStatus)
        assertEquals("Mirandola (MO)", state.locationLabel)
        assertEquals(listOf(1L), state.stations.map { it.station.id })
        assertNotNull(state.stations.single().distanceKm)
        assertTrue(state.stations.single().distanceKm!! < 25.0)
    }

    private class CachedStationsLocationRepository : NearbyStationsRepository {
        var resolveLocationCalls = 0

        private val stations = listOf(
            station(1, "Mirandola", 44.8870, 11.0660),
            station(2, "Livigno", 46.5380, 10.1350),
        )
        private val snapshot = NearbyStationsSnapshot(
            extractionDate = LocalDate.of(2026, 9, 3),
            pricesExtractionDate = LocalDate.of(2026, 9, 3),
            rankedStations = RankedEniStations(
                locationResult = UserLocationResult.Unavailable,
                stations = EniStationDistanceRanker.rank(stations, null),
            ),
            lastSuccessfulUpdateEpochMillis = 1L,
        )

        override fun observeStations(): Flow<NearbyStationsSnapshot?> = flowOf(snapshot)

        override suspend fun loadCachedSnapshot(): NearbyStationsSnapshot = snapshot

        override suspend fun resolveLocation(): UserLocationResult {
            resolveLocationCalls += 1
            return UserLocationResult.Available(GeoPoint(44.8870, 11.0660))
        }

        override suspend fun refresh(): MimitRefreshResult = MimitRefreshResult.Failure(retryable = false)
    }

    companion object {
        private fun station(
            id: Long,
            municipality: String,
            latitude: Double,
            longitude: Double,
        ): MimitStation = MimitStation(
            id = id,
            manager = "manager",
            brand = "Eni",
            stationType = "stradale",
            name = "Eni $municipality",
            address = "Via Test",
            municipality = municipality,
            province = if (municipality == "Mirandola") "MO" else "SO",
            latitude = latitude,
            longitude = longitude,
        )
    }
}
