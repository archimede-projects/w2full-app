package com.archimede.w2full.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineDistance {
    private const val EARTH_MEAN_RADIUS_KM = 6371.0088

    fun kilometers(from: GeoPoint, to: GeoPoint): Double {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)

        val sinLatitude = sin(latitudeDelta / 2.0)
        val sinLongitude = sin(longitudeDelta / 2.0)
        val a = (
            sinLatitude * sinLatitude +
                cos(fromLatitude) * cos(toLatitude) * sinLongitude * sinLongitude
            ).coerceIn(0.0, 1.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_MEAN_RADIUS_KM * c
    }
}
