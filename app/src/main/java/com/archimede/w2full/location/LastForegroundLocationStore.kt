package com.archimede.w2full.location

import android.content.Context

data class StoredUserLocation(
    val point: GeoPoint,
    val observedAtEpochMillis: Long,
)

interface LastForegroundLocationStore {
    fun load(): StoredUserLocation?
    fun save(point: GeoPoint, observedAtEpochMillis: Long)
}

class SharedPreferencesLastForegroundLocationStore(context: Context) : LastForegroundLocationStore {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): StoredUserLocation? {
        if (!preferences.contains(KEY_LATITUDE) || !preferences.contains(KEY_LONGITUDE)) return null
        val latitude = Double.fromBits(preferences.getLong(KEY_LATITUDE, 0L))
        val longitude = Double.fromBits(preferences.getLong(KEY_LONGITUDE, 0L))
        val observedAt = preferences.getLong(KEY_OBSERVED_AT, 0L)
        if (observedAt <= 0L) return null
        val point = runCatching { GeoPoint(latitude, longitude) }.getOrNull() ?: return null
        return StoredUserLocation(point, observedAt)
    }

    override fun save(point: GeoPoint, observedAtEpochMillis: Long) {
        if (observedAtEpochMillis <= 0L) return
        preferences.edit()
            .putLong(KEY_LATITUDE, point.latitude.toBits())
            .putLong(KEY_LONGITUDE, point.longitude.toBits())
            .putLong(KEY_OBSERVED_AT, observedAtEpochMillis)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "last_foreground_location"
        private const val KEY_LATITUDE = "latitude_bits"
        private const val KEY_LONGITUDE = "longitude_bits"
        private const val KEY_OBSERVED_AT = "observed_at_epoch_millis"
    }
}

class InMemoryLastForegroundLocationStore(
    initial: StoredUserLocation? = null,
) : LastForegroundLocationStore {
    private var value = initial

    override fun load(): StoredUserLocation? = value

    override fun save(point: GeoPoint, observedAtEpochMillis: Long) {
        value = StoredUserLocation(point, observedAtEpochMillis)
    }
}
