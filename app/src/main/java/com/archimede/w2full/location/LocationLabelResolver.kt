package com.archimede.w2full.location

import android.content.Context
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface LocationLabelResolver {
    suspend fun resolve(point: GeoPoint): String?
}

object NoOpLocationLabelResolver : LocationLabelResolver {
    override suspend fun resolve(point: GeoPoint): String? = null
}

class AndroidGeocoderLocationLabelResolver(
    context: Context,
    private val geocoder: Geocoder = Geocoder(context.applicationContext, Locale.ITALY),
) : LocationLabelResolver {
    @Suppress("DEPRECATION")
    override suspend fun resolve(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        runCatching {
            val address = geocoder.getFromLocation(point.latitude, point.longitude, 1)
                ?.firstOrNull()
                ?: return@runCatching null
            val locality = address.locality
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: address.subLocality?.trim()?.takeIf { it.isNotEmpty() }
            val province = address.subAdminArea
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: address.adminArea?.trim()?.takeIf { it.isNotEmpty() }

            when {
                locality != null && province != null && !locality.equals(province, ignoreCase = true) -> "$locality ($province)"
                locality != null -> locality
                province != null -> province
                else -> null
            }
        }.getOrNull()
    }
}
