package com.archimede.w2full.ui.history

import android.content.Context
import com.archimede.w2full.data.repository.PriceHistoryStation

internal const val HISTORY_FAVORITES_PREFS_NAME = "history_favorite_stations"
private const val HISTORY_FAVORITES_KEY = "station_ids"

interface HistoryFavoriteStationsStore {
    fun load(): Set<Long>
    fun save(stationIds: Set<Long>)
}

class SharedPreferencesHistoryFavoriteStationsStore(
    context: Context,
) : HistoryFavoriteStationsStore {
    private val preferences = context.getSharedPreferences(
        HISTORY_FAVORITES_PREFS_NAME,
        Context.MODE_PRIVATE,
    )

    override fun load(): Set<Long> = preferences
        .getStringSet(HISTORY_FAVORITES_KEY, emptySet())
        .orEmpty()
        .mapNotNull { it.toLongOrNull() }
        .toSet()

    override fun save(stationIds: Set<Long>) {
        preferences.edit()
            .putStringSet(HISTORY_FAVORITES_KEY, stationIds.map { it.toString() }.toSet())
            .apply()
    }
}

class InMemoryHistoryFavoriteStationsStore(
    initialStationIds: Set<Long> = emptySet(),
) : HistoryFavoriteStationsStore {
    private var stationIds: Set<Long> = initialStationIds.toSet()

    override fun load(): Set<Long> = stationIds.toSet()

    override fun save(stationIds: Set<Long>) {
        this.stationIds = stationIds.toSet()
    }
}

internal data class HistoryStationGroups(
    val favorites: List<PriceHistoryStation>,
    val others: List<PriceHistoryStation>,
)

internal fun groupHistoryStations(
    stations: List<PriceHistoryStation>,
    favoriteStationIds: Set<Long>,
): HistoryStationGroups = HistoryStationGroups(
    favorites = stations.filter { it.stationId in favoriteStationIds },
    others = stations.filterNot { it.stationId in favoriteStationIds },
)

internal fun historyStationsForFavorites(
    stations: List<PriceHistoryStation>,
    favoriteStationIds: Set<Long>,
): List<PriceHistoryStation> = stations.filter { it.stationId in favoriteStationIds }

internal fun resolveHistoryStationSelection(
    currentStationId: Long?,
    stations: List<PriceHistoryStation>,
    favoriteStationIds: Set<Long>,
): Long? {
    if (currentStationId != null && stations.any { it.stationId == currentStationId }) {
        return currentStationId
    }
    return stations.firstOrNull { it.stationId in favoriteStationIds }?.stationId
        ?: stations.firstOrNull()?.stationId
}

internal fun toggledHistoryFavoriteStationIds(
    current: Set<Long>,
    stationId: Long,
): Set<Long> = if (stationId in current) {
    current - stationId
} else {
    current + stationId
}
