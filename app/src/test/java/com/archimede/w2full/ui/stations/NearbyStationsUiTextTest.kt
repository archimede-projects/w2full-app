package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitStation
import com.archimede.w2full.data.mimit.MimitStationDistance
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class NearbyStationsUiTextTest {
    @Test
    fun locationStatusLabelsCoverAvailableDeniedAndUnavailable() {
        assertEquals("Posizione disponibile", locationStatusTitle(NearbyLocationUiStatus.AVAILABLE))
        assertEquals("Stazioni ordinate per distanza", locationStatusSubtitle(NearbyLocationUiStatus.AVAILABLE))

        assertEquals("Permesso posizione negato", locationStatusTitle(NearbyLocationUiStatus.PERMISSION_DENIED))
        assertEquals("Stazioni ordinate alfabeticamente", locationStatusSubtitle(NearbyLocationUiStatus.PERMISSION_DENIED))

        assertEquals("Posizione non disponibile", locationStatusTitle(NearbyLocationUiStatus.UNAVAILABLE))
        assertEquals("Stazioni ordinate alfabeticamente", locationStatusSubtitle(NearbyLocationUiStatus.UNAVAILABLE))
    }

    @Test
    fun lastUpdatePlaceholderRemainsOnlyBeforeFirstSuccessfulImport() {
        assertEquals(
            "Ultimo aggiornamento: non ancora disponibile",
            lastUpdateLabel(null),
        )
    }

    @Test
    fun lastUpdateShowsRelativeAgeAndAbsoluteTimestamp() {
        val now = Instant.parse("2026-09-02T07:00:00Z").toEpochMilli()

        assertEquals(
            "Aggiornato pochi minuti fa · 02/09/2026 06:30",
            lastUpdateLabel(
                Instant.parse("2026-09-02T06:30:00Z").toEpochMilli(),
                now,
                ZoneOffset.UTC,
            ),
        )
        assertEquals(
            "Aggiornato 6 ore fa · 02/09/2026 01:00",
            lastUpdateLabel(
                Instant.parse("2026-09-02T01:00:00Z").toEpochMilli(),
                now,
                ZoneOffset.UTC,
            ),
        )
        assertEquals(
            "Aggiornato 3 giorni fa · 30/08/2026 07:00",
            lastUpdateLabel(
                Instant.parse("2026-08-30T07:00:00Z").toEpochMilli(),
                now,
                ZoneOffset.UTC,
            ),
        )
    }

    @Test
    fun refreshFailureKeepsCachedStationsAndTimestampAndUsesOnlyGenericMessage() {
        val cachedStation = MimitStationDistance(
            station = MimitStation(
                id = 1,
                manager = "Gestore",
                brand = "Eni",
                stationType = "Stradale",
                name = "Cached",
                address = "Via Roma",
                municipality = "Roma",
                province = "RM",
                latitude = null,
                longitude = null,
            ),
            distanceKm = null,
        )
        val timestamp = Instant.parse("2026-09-01T07:00:00Z").toEpochMilli()
        val state = NearbyStationsUiState(
            stations = listOf(cachedStation),
            lastSuccessfulUpdateEpochMillis = timestamp,
        ).withRefreshFailure()

        assertEquals(listOf(1L), state.stations.map { it.station.id })
        assertEquals(timestamp, state.lastSuccessfulUpdateEpochMillis)
        assertEquals(MIMIT_REFRESH_ERROR_MESSAGE, state.errorMessage)
        assertFalse(state.errorMessage!!.contains("Exception", ignoreCase = true))
        assertFalse(state.errorMessage!!.contains("header", ignoreCase = true))
    }
}
