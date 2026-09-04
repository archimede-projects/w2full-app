package com.archimede.w2full.ui.stations

import com.archimede.w2full.data.mimit.MimitStationDistance
import com.archimede.w2full.data.mimit.MimitStationFuelPrice

internal fun filterAndSortStations(
    stations: List<MimitStationDistance>,
    pricesByStationId: Map<Long, MimitStationFuelPrice>,
    locationStatus: NearbyLocationUiStatus?,
    preferences: StationListPreferences,
    favoriteStationIds: Set<Long> = emptySet(),
): List<MimitStationDistance> {
    val radiusFilteringActive =
        preferences.radiusEnabled && locationStatus == NearbyLocationUiStatus.AVAILABLE

    val radiusFiltered = if (radiusFilteringActive) {
        stations.filter { station ->
            station.distanceKm?.let { it <= preferences.radiusKm.toDouble() } == true
        }
    } else {
        stations
    }

    val scoped = when (preferences.scope) {
        StationListScope.ALL -> radiusFiltered
        StationListScope.FAVORITES -> radiusFiltered.filter { it.station.id in favoriteStationIds }
    }

    return when (preferences.sortMode) {
        StationSortMode.DISTANCE -> scoped
        StationSortMode.SELF_PRICE -> scoped.sortedWith(
            stationPriceComparator(pricesByStationId, StationSortMode.SELF_PRICE),
        )
        StationSortMode.SERVED_PRICE -> scoped.sortedWith(
            stationPriceComparator(pricesByStationId, StationSortMode.SERVED_PRICE),
        )
    }
}

private fun stationPriceComparator(
    pricesByStationId: Map<Long, MimitStationFuelPrice>,
    sortMode: StationSortMode,
): Comparator<MimitStationDistance> = Comparator { left, right ->
    val leftPrice = pricesByStationId[left.station.id]?.priceFor(sortMode)
    val rightPrice = pricesByStationId[right.station.id]?.priceFor(sortMode)

    val priceComparison = compareNullableLong(leftPrice, rightPrice)
    if (priceComparison != 0) {
        return@Comparator priceComparison
    }

    val distanceComparison = compareNullableDouble(left.distanceKm, right.distanceKm)
    if (distanceComparison != 0) {
        return@Comparator distanceComparison
    }

    left.station.id.compareTo(right.station.id)
}

private fun MimitStationFuelPrice.priceFor(sortMode: StationSortMode): Long? = when (sortMode) {
    StationSortMode.DISTANCE -> null
    StationSortMode.SELF_PRICE -> self?.priceMilliEuroPerUnit
    StationSortMode.SERVED_PRICE -> served?.priceMilliEuroPerUnit
}

private fun compareNullableLong(left: Long?, right: Long?): Int = when {
    left == null && right == null -> 0
    left == null -> 1
    right == null -> -1
    else -> left.compareTo(right)
}

private fun compareNullableDouble(left: Double?, right: Double?): Int = when {
    left == null && right == null -> 0
    left == null -> 1
    right == null -> -1
    else -> left.compareTo(right)
}