package com.archimede.w2full.data.mimit

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MimitStationPriceSelectorTest {
    @Test
    fun exactNormalizedFuelMatchAcceptsCaseAndWhitespaceButRejectsSubstrings() {
        val prices = listOf(
            price(1, "  BENZINA   ", 1_759, true, "2026-09-02T08:00:00"),
            price(2, "Benzina Plus", 1_650, true, "2026-09-02T08:00:00"),
            price(3, "SuperBenzina", 1_640, true, "2026-09-02T08:00:00"),
        )

        val result = MimitStationPriceSelector.select(prices, " benzina ")

        assertEquals("benzina", result.fuelType)
        assertTrue(result.pricesByStationId.containsKey(1))
        assertFalse(result.pricesByStationId.containsKey(2))
        assertFalse(result.pricesByStationId.containsKey(3))
    }

    @Test
    fun selfAndServedAreBothSelectedAndNewestCommunicationWinsPerMode() {
        val prices = listOf(
            price(10, "Benzina", 1_700, true, "2026-09-01T08:00:00"),
            price(10, "Benzina", 1_759, true, "2026-09-02T08:00:00"),
            price(10, "Benzina", 1_850, false, "2026-09-01T08:00:00"),
            price(10, "Benzina", 1_899, false, "2026-09-02T09:00:00"),
        )

        val selected = requireNotNull(
            MimitStationPriceSelector.select(prices, "Benzina").pricesByStationId[10],
        )

        assertEquals(1_759, selected.self?.priceMilliEuroPerUnit)
        assertEquals(LocalDateTime.parse("2026-09-02T08:00:00"), selected.self?.communicatedAt)
        assertEquals(1_899, selected.served?.priceMilliEuroPerUnit)
        assertEquals(LocalDateTime.parse("2026-09-02T09:00:00"), selected.served?.communicatedAt)
    }

    @Test
    fun singleModeIsKeptAndDifferentFuelIsIgnored() {
        val prices = listOf(
            price(20, "Gasolio", 1_650, true, "2026-09-02T08:00:00"),
            price(20, "Benzina", 1_999, false, "2026-09-02T09:00:00"),
        )

        val selected = requireNotNull(
            MimitStationPriceSelector.select(prices, "Gasolio").pricesByStationId[20],
        )

        assertEquals(1_650, selected.self?.priceMilliEuroPerUnit)
        assertNull(selected.served)
    }

    @Test
    fun metanoUsesKilogramAndMissingVehicleFuelFallsBackToBenzina() {
        val metano = requireNotNull(
            MimitStationPriceSelector.select(
                listOf(price(30, "Metano", 1_299, true, "2026-09-02T08:00:00")),
                "  METANO  ",
            ).pricesByStationId[30],
        )
        val fallback = MimitStationPriceSelector.select(
            listOf(price(31, "Benzina", 1_759, true, "2026-09-02T08:00:00")),
            "   ",
        )

        assertEquals(MimitPriceUnit.KILOGRAM, metano.unit)
        assertEquals(MimitStationPriceSelector.FALLBACK_FUEL_TYPE, fallback.fuelType)
        assertTrue(fallback.pricesByStationId.containsKey(31))
    }

    @Test
    fun noCompatibleFuelProducesNoStationPrice() {
        val result = MimitStationPriceSelector.select(
            listOf(price(40, "Gasolio", 1_650, true, "2026-09-02T08:00:00")),
            "Benzina",
        )

        assertTrue(result.pricesByStationId.isEmpty())
    }

    private fun price(
        stationId: Long,
        fuelDescription: String,
        milliEuro: Long,
        isSelf: Boolean,
        communicatedAt: String,
    ) = MimitPrice(
        stationId = stationId,
        fuelDescription = fuelDescription,
        priceMilliEuroPerUnit = milliEuro,
        isSelf = isSelf,
        communicatedAt = LocalDateTime.parse(communicatedAt),
    )
}
