package com.archimede.w2full.location

sealed interface UserLocationResult {
    data class Available(val point: GeoPoint) : UserLocationResult

    data object PermissionDenied : UserLocationResult

    data object Unavailable : UserLocationResult
}

interface UserLocationProvider {
    suspend fun currentLocation(): UserLocationResult
}
