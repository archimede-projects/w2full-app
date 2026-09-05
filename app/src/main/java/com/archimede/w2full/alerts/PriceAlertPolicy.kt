package com.archimede.w2full.alerts

import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.repository.PriceAlertRule
import com.archimede.w2full.location.GeoPoint
import com.archimede.w2full.location.HaversineDistance
import java.security.MessageDigest

data class PriceAlertCandidate(
    val stationId: Long,
    val stationName: String,
    val priceMilliEuroPerUnit: Long,
    val distanceKm: Double?,
)

data class PriceAlertSelection(
    val candidates: List<PriceAlertCandidate>,
    val fingerprint: String?,
)

fun selectPriceAlertCandidates(
    rule: PriceAlertRule,
    stations: List<MimitStationEntity>,
    prices: List<MimitPriceEntity>,
    location: GeoPoint?,
): PriceAlertSelection {
    if (rule.radiusKm != null && location == null) return PriceAlertSelection(emptyList(), null)

    val stationsById = stations.associateBy { it.stationId }
    val bestMatchingPrices = prices.asSequence()
        .filter { it.fuelDescription.equals(rule.fuelDescription, ignoreCase = true) }
        .filter { it.isSelf == rule.isSelf }
        .filter { it.priceMilliEuroPerUnit <= rule.maxPriceMilliEuroPerUnit }
        .groupBy { it.stationId }
        .mapValues { (_, rows) -> rows.minBy { it.priceMilliEuroPerUnit } }

    val candidates = bestMatchingPrices.mapNotNull { (stationId, price) ->
        val station = stationsById[stationId] ?: return@mapNotNull null
        val distance = location?.let { origin ->
            val latitude = station.latitude ?: return@let null
            val longitude = station.longitude ?: return@let null
            val destination = runCatching { GeoPoint(latitude, longitude) }.getOrNull() ?: return@let null
            HaversineDistance.kilometers(origin, destination)
        }
        if (rule.radiusKm != null && (distance == null || distance > rule.radiusKm.toDouble())) {
            return@mapNotNull null
        }
        PriceAlertCandidate(
            stationId = stationId,
            stationName = station.name.ifBlank { "Eni #$stationId" },
            priceMilliEuroPerUnit = price.priceMilliEuroPerUnit,
            distanceKm = distance,
        )
    }.sortedWith(
        compareBy<PriceAlertCandidate> { it.priceMilliEuroPerUnit }
            .thenBy { it.distanceKm ?: Double.POSITIVE_INFINITY }
            .thenBy { it.stationId },
    )

    return PriceAlertSelection(
        candidates = candidates,
        fingerprint = candidates.takeIf { it.isNotEmpty() }?.let(::priceAlertFingerprint),
    )
}

fun priceAlertFingerprint(candidates: List<PriceAlertCandidate>): String {
    val canonical = candidates.map { it.stationId }.distinct().sorted().joinToString(",")
    val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
