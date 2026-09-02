package com.archimede.w2full.ui.stations

import org.junit.Assert.assertEquals
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
    fun lastUpdatePlaceholderIsReservedBeforeM45Persistence() {
        assertEquals(
            "Ultimo aggiornamento: non ancora disponibile",
            lastUpdateLabel(null),
        )
    }
}
