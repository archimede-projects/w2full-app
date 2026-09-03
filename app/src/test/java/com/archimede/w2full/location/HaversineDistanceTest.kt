package com.archimede.w2full.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HaversineDistanceTest {
    @Test
    fun samePointHasZeroDistance() {
        val rome = GeoPoint(41.9028, 12.4964)

        assertEquals(0.0, HaversineDistance.kilometers(rome, rome), 1e-9)
    }

    @Test
    fun romeToMilanMatchesKnownGreatCircleDistance() {
        val rome = GeoPoint(41.9028, 12.4964)
        val milan = GeoPoint(45.4642, 9.1900)

        assertEquals(476.885, HaversineDistance.kilometers(rome, milan), 0.5)
    }

    @Test
    fun distanceIsSymmetric() {
        val first = GeoPoint(40.8518, 14.2681)
        val second = GeoPoint(45.0703, 7.6869)

        assertEquals(
            HaversineDistance.kilometers(first, second),
            HaversineDistance.kilometers(second, first),
            1e-9,
        )
    }

    @Test
    fun geoPointRejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(90.1, 12.0) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(45.0, -180.1) }
        assertThrows(IllegalArgumentException::class.java) { GeoPoint(Double.NaN, 12.0) }
    }
}
