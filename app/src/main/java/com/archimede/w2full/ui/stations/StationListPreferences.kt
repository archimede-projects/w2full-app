package com.archimede.w2full.ui.stations

import android.content.Context

enum class StationSortMode {
    DISTANCE,
    SELF_PRICE,
    SERVED_PRICE,
}

data class StationListPreferences(
    val radiusEnabled: Boolean = false,
    val radiusKm: Int = DEFAULT_RADIUS_KM,
    val sortMode: StationSortMode = StationSortMode.DISTANCE,
) {
    companion object {
        const val MIN_RADIUS_KM = 1
        const val MAX_RADIUS_KM = 200
        const val DEFAULT_RADIUS_KM = 20
    }
}

interface StationListPreferencesStore {
    fun load(): StationListPreferences
    fun save(preferences: StationListPreferences)
}

class SharedPreferencesStationListPreferencesStore(
    context: Context,
) : StationListPreferencesStore {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): StationListPreferences {
        val radiusKm = preferences
            .getInt(KEY_RADIUS_KM, StationListPreferences.DEFAULT_RADIUS_KM)
            .coerceIn(
                StationListPreferences.MIN_RADIUS_KM,
                StationListPreferences.MAX_RADIUS_KM,
            )
        val sortMode = preferences.getString(KEY_SORT_MODE, null)
            ?.let { raw -> runCatching { StationSortMode.valueOf(raw) }.getOrNull() }
            ?: StationSortMode.DISTANCE
        return StationListPreferences(
            radiusEnabled = preferences.getBoolean(KEY_RADIUS_ENABLED, false),
            radiusKm = radiusKm,
            sortMode = sortMode,
        )
    }

    override fun save(preferences: StationListPreferences) {
        this.preferences.edit()
            .putBoolean(KEY_RADIUS_ENABLED, preferences.radiusEnabled)
            .putInt(
                KEY_RADIUS_KM,
                preferences.radiusKm.coerceIn(
                    StationListPreferences.MIN_RADIUS_KM,
                    StationListPreferences.MAX_RADIUS_KM,
                ),
            )
            .putString(KEY_SORT_MODE, preferences.sortMode.name)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "station_list_preferences"
        const val KEY_RADIUS_ENABLED = "radius_enabled"
        const val KEY_RADIUS_KM = "radius_km"
        const val KEY_SORT_MODE = "sort_mode"
    }
}

internal class InMemoryStationListPreferencesStore(
    initial: StationListPreferences = StationListPreferences(),
) : StationListPreferencesStore {
    private var current = initial

    override fun load(): StationListPreferences = current

    override fun save(preferences: StationListPreferences) {
        current = preferences
    }
}

internal fun validatedRadiusOrPrevious(
    input: String,
    previousValidRadiusKm: Int,
): Int {
    val parsed = input.trim().toIntOrNull()
    return if (
        parsed != null &&
        parsed in StationListPreferences.MIN_RADIUS_KM..StationListPreferences.MAX_RADIUS_KM
    ) {
        parsed
    } else {
        previousValidRadiusKm
    }
}

internal fun isValidRadiusInput(input: String): Boolean =
    input.trim().toIntOrNull()
        ?.let { it in StationListPreferences.MIN_RADIUS_KM..StationListPreferences.MAX_RADIUS_KM }
        ?: false
