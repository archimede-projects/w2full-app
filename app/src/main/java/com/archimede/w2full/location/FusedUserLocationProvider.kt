package com.archimede.w2full.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FusedUserLocationProvider(
    context: Context,
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context.applicationContext),
) : UserLocationProvider {
    private val applicationContext = context.applicationContext

    override suspend fun currentLocation(): UserLocationResult {
        if (!hasLocationPermission()) {
            return UserLocationResult.PermissionDenied
        }

        return try {
            val location = requestCurrentLocation()
            if (location == null) {
                UserLocationResult.Unavailable
            } else {
                val point = runCatching { GeoPoint(location.latitude, location.longitude) }.getOrNull()
                if (point == null) {
                    UserLocationResult.Unavailable
                } else {
                    UserLocationResult.Available(point)
                }
            }
        } catch (_: SecurityException) {
            UserLocationResult.PermissionDenied
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            UserLocationResult.Unavailable
        }
    }

    private fun hasLocationPermission(): Boolean =
        applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            applicationContext.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellationSource = CancellationTokenSource()
        continuation.invokeOnCancellation { cancellationSource.cancel() }

        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token)
            .addOnSuccessListener { location ->
                if (continuation.isActive) {
                    continuation.resume(location)
                }
            }
            .addOnFailureListener { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
            .addOnCanceledListener {
                if (continuation.isActive) {
                    continuation.cancel(CancellationException("Location request cancelled"))
                }
            }
    }
}
