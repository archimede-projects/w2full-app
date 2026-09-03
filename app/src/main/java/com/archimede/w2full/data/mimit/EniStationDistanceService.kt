package com.archimede.w2full.data.mimit

import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.HaversineDistance
import com.archimede.w2full.location.UserLocationProvider
import com.archimede.w2full.location.UserLocationResult
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

data class MimitStationDistance(
    val station: MimitStation,
    val distanceKm: Double?,
)

data class RankedEniStations(
    val locationResult: UserLocationResult,
    val stations: List<MimitStationDistance>,
)

class EniStationDistanceService(
    private val userLocationProvider: UserLocationProvider,
    private val locationTimeoutMillis: Long = DEFAULT_LOCATION_TIMEOUT_MILLIS,
) {
    init {
        require(locationTimeoutMillis > 0L) { "locationTimeoutMillis must be positive" }
    }

    suspend fun resolveLocation(): UserLocationResult = try {
        withTimeoutOrNull(locationTimeoutMillis) {
            userLocationProvider.currentLocation()
        } ?: UserLocationResult.Unavailable
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        UserLocationResult.Unavailable
    }

    suspend fun rank(stations: List<MimitStation>): RankedEniStations {
        val locationResult = resolveLocation()
        val userPoint = (locationResult as? UserLocationResult.Available)?.point
        return RankedEniStations(
            locationResult = locationResult,
            stations = EniStationDistanceRanker.rank(stations, userPoint),
        )
    }

    companion object {
        const val DEFAULT_LOCATION_TIMEOUT_MILLIS = 12_000L
    }
}

object EniStationDistanceRanker {
    fun rank(
        stations: List<MimitStation>,
        userLocation: GeoPoint?,
    ): List<MimitStationDistance> {
        val ranked = MimitStationFilter.eniStations(stations).map { station ->
            MimitStationDistance(
                station = station,
                distanceKm = userLocation?.let { origin ->
                    station.geoPointOrNull()?.let { destination ->
                        HaversineDistance.kilometers(origin, destination)
                    }
                },
            )
        }

        return if (userLocation == null) {
            ranked.sortedWith(ALPHABETICAL_COMPARATOR)
        } else {
            ranked.sortedWith(DISTANCE_COMPARATOR)
        }
    }

    private fun MimitStation.geoPointOrNull(): GeoPoint? {
        val latitude = latitude ?: return null
        val longitude = longitude ?: return null
        return runCatching { GeoPoint(latitude, longitude) }.getOrNull()
    }

    private fun primarySortLabel(station: MimitStation): String = when {
        station.name.isNotBlank() -> station.name
        station.address.isNotBlank() -> station.address
        station.municipality.isNotBlank() -> station.municipality
        else -> station.id.toString()
    }

    private fun normalize(value: String): String = value
        .trim()
        .replace(WHITESPACE, " ")
        .lowercase(Locale.ROOT)

    private val ALPHABETICAL_COMPARATOR = Comparator<MimitStationDistance> { left, right ->
        compareValuesBy(
            left,
            right,
            { normalize(primarySortLabel(it.station)) },
            { normalize(it.station.municipality) },
            { normalize(it.station.address) },
            { it.station.id },
        )
    }

    private val DISTANCE_COMPARATOR = Comparator<MimitStationDistance> { left, right ->
        val leftDistance = left.distanceKm
        val rightDistance = right.distanceKm

        when {
            leftDistance == null && rightDistance == null -> ALPHABETICAL_COMPARATOR.compare(left, right)
            leftDistance == null -> 1
            rightDistance == null -> -1
            else -> {
                val distanceComparison = leftDistance.compareTo(rightDistance)
                if (distanceComparison != 0) {
                    distanceComparison
                } else {
                    ALPHABETICAL_COMPARATOR.compare(left, right)
                }
            }
        }
    }

    private val WHITESPACE = Regex("\\s+")
}
