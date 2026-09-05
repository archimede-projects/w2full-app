package com.archimede.w2full.alerts

import com.archimede.w2full.data.local.MimitPriceEntity
import com.archimede.w2full.data.local.MimitStationEntity
import com.archimede.w2full.data.repository.PriceAlertRule
import com.archimede.w2full.location.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PriceAlertPolicyTest {
    @Test
    fun filterIncludesThresholdAndCorrectServiceOnly() {
        val rule = rule(maxPrice = 1_800, isSelf = true, radiusKm = null)
        val selection = selectPriceAlertCandidates(
            rule = rule,
            stations = listOf(station(1), station(2), station(3)),
            prices = listOf(
                price(1, 1_800, isSelf = true),
                price(2, 1_801, isSelf = true),
                price(3, 1_700, isSelf = false),
            ),
            location = null,
        )
        assertEquals(listOf(1L), selection.candidates.map { it.stationId })
    }

    @Test
    fun radiusUsesHaversineAndSortsByBestPrice() {
        val origin = GeoPoint(44.836, 11.293)
        val selection = selectPriceAlertCandidates(
            rule = rule(maxPrice = 2_000, isSelf = true, radiusKm = 10),
            stations = listOf(
                station(1, latitude = 44.84, longitude = 11.30),
                station(2, latitude = 44.85, longitude = 11.31),
                station(3, latitude = 45.50, longitude = 10.90),
            ),
            prices = listOf(price(1, 1_900), price(2, 1_750), price(3, 1_600)),
            location = origin,
        )
        assertEquals(listOf(2L, 1L), selection.candidates.map { it.stationId })
    }

    @Test
    fun fingerprintIsStableForSameStationSetAndChangesWithSet() {
        val a = listOf(
            PriceAlertCandidate(2, "B", 1700, null),
            PriceAlertCandidate(1, "A", 1800, null),
        )
        val b = a.reversed()
        assertEquals(priceAlertFingerprint(a), priceAlertFingerprint(b))
        assertNotEquals(
            priceAlertFingerprint(a),
            priceAlertFingerprint(a + PriceAlertCandidate(3, "C", 1600, null)),
        )
    }

    private fun rule(maxPrice: Long, isSelf: Boolean, radiusKm: Int?) = PriceAlertRule(
        fuelDescription = "Benzina",
        maxPriceMilliEuroPerUnit = maxPrice,
        isSelf = isSelf,
        radiusKm = radiusKm,
        isActive = true,
        lastNotifiedFingerprint = null,
        lastNotifiedAtEpochMillis = null,
        updatedAtEpochMillis = 1L,
    )

    private fun station(
        id: Long,
        latitude: Double? = 44.84,
        longitude: Double? = 11.30,
    ) = MimitStationEntity(
        stationId = id,
        manager = "Gestore",
        brand = "Eni",
        stationType = "Stradale",
        name = "Eni $id",
        address = "Via $id",
        municipality = "Test",
        province = "MO",
        latitude = latitude,
        longitude = longitude,
    )

    private fun price(
        stationId: Long,
        value: Long,
        isSelf: Boolean = true,
    ) = MimitPriceEntity(
        stationId = stationId,
        fuelDescription = "Benzina",
        priceMilliEuroPerUnit = value,
        isSelf = isSelf,
        communicatedAt = "2026-09-05T08:00:00",
    )
}
