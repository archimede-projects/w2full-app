package com.archimede.w2full.ui.history

import android.content.Context
import com.archimede.w2full.data.repository.PriceHistoryPoint
import java.time.LocalDateTime

enum class HistoryStationScope {
    FAVORITES,
    OTHERS,
}

enum class HistoryPeriod(val months: Long?) {
    ONE_MONTH(1),
    THREE_MONTHS(3),
    SIX_MONTHS(6),
    ONE_YEAR(12),
    ALL(null),
}

data class HistoryPreferences(
    val stationScope: HistoryStationScope = HistoryStationScope.FAVORITES,
    val seriesAFuelType: String? = null,
    val seriesAIsSelf: Boolean = true,
    val seriesBEnabled: Boolean = false,
    val seriesBFuelType: String? = null,
    val seriesBIsSelf: Boolean = true,
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val showTable: Boolean = false,
)

interface HistoryPreferencesStore {
    fun load(): HistoryPreferences
    fun save(preferences: HistoryPreferences)
}

class SharedPreferencesHistoryPreferencesStore(context: Context) : HistoryPreferencesStore {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): HistoryPreferences = HistoryPreferences(
        stationScope = enumValueOrDefault(
            preferences.getString(KEY_STATION_SCOPE, null),
            HistoryStationScope.FAVORITES,
        ),
        seriesAFuelType = preferences.getString(KEY_SERIES_A_FUEL, null),
        seriesAIsSelf = preferences.getBoolean(KEY_SERIES_A_SELF, true),
        seriesBEnabled = preferences.getBoolean(KEY_SERIES_B_ENABLED, false),
        seriesBFuelType = preferences.getString(KEY_SERIES_B_FUEL, null),
        seriesBIsSelf = preferences.getBoolean(KEY_SERIES_B_SELF, true),
        period = enumValueOrDefault(preferences.getString(KEY_PERIOD, null), HistoryPeriod.ALL),
        showTable = preferences.getBoolean(KEY_SHOW_TABLE, false),
    )

    override fun save(preferences: HistoryPreferences) {
        this.preferences.edit()
            .putString(KEY_STATION_SCOPE, preferences.stationScope.name)
            .putString(KEY_SERIES_A_FUEL, preferences.seriesAFuelType)
            .putBoolean(KEY_SERIES_A_SELF, preferences.seriesAIsSelf)
            .putBoolean(KEY_SERIES_B_ENABLED, preferences.seriesBEnabled)
            .putString(KEY_SERIES_B_FUEL, preferences.seriesBFuelType)
            .putBoolean(KEY_SERIES_B_SELF, preferences.seriesBIsSelf)
            .putString(KEY_PERIOD, preferences.period.name)
            .putBoolean(KEY_SHOW_TABLE, preferences.showTable)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, fallback: T): T =
        raw?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback

    private companion object {
        const val PREFS_NAME = "price_history_preferences"
        const val KEY_STATION_SCOPE = "station_scope"
        const val KEY_SERIES_A_FUEL = "series_a_fuel"
        const val KEY_SERIES_A_SELF = "series_a_self"
        const val KEY_SERIES_B_ENABLED = "series_b_enabled"
        const val KEY_SERIES_B_FUEL = "series_b_fuel"
        const val KEY_SERIES_B_SELF = "series_b_self"
        const val KEY_PERIOD = "period"
        const val KEY_SHOW_TABLE = "show_table"
    }
}

internal class InMemoryHistoryPreferencesStore(
    initial: HistoryPreferences = HistoryPreferences(),
) : HistoryPreferencesStore {
    private var current = initial
    override fun load(): HistoryPreferences = current
    override fun save(preferences: HistoryPreferences) {
        current = preferences
    }
}

internal fun filterHistoryPointsByPeriod(
    points: List<PriceHistoryPoint>,
    period: HistoryPeriod,
    now: LocalDateTime = LocalDateTime.now(),
): List<PriceHistoryPoint> {
    val months = period.months ?: return points
    val threshold = now.minusMonths(months)
    return points.filter { !it.communicatedAt.isBefore(threshold) }
}

data class HistoryTableRow(
    val communicatedAt: LocalDateTime,
    val seriesAPriceMilliEuroPerUnit: Long?,
    val seriesBPriceMilliEuroPerUnit: Long?,
)

internal fun mergeHistorySeriesRows(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
): List<HistoryTableRow> {
    val aByDate = seriesA.associateBy { it.communicatedAt }
    val bByDate = seriesB.associateBy { it.communicatedAt }
    return (aByDate.keys + bByDate.keys)
        .distinct()
        .sorted()
        .map { date ->
            HistoryTableRow(
                communicatedAt = date,
                seriesAPriceMilliEuroPerUnit = aByDate[date]?.priceMilliEuroPerUnit,
                seriesBPriceMilliEuroPerUnit = bByDate[date]?.priceMilliEuroPerUnit,
            )
        }
}