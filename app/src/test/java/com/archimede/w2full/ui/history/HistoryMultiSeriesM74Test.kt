package com.archimede.w2full.ui.history

import com.archimede.w2full.data.repository.PriceHistoryPoint
import com.archimede.w2full.data.repository.PriceHistoryStation
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryMultiSeriesM74Test {
    @Test
    fun `favorite and other history scopes are disjoint`() {
        val stations = listOf(station(1), station(2), station(3))
        assertEquals(
            listOf(1L, 3L),
            historyStationsForScope(stations, setOf(1L, 3L), HistoryStationScope.FAVORITES)
                .map { it.stationId },
        )
        assertEquals(
            listOf(2L),
            historyStationsForScope(stations, setOf(1L, 3L), HistoryStationScope.OTHERS)
                .map { it.stationId },
        )
    }

    @Test
    fun `history stations use distance when available and stable name otherwise`() {
        val stations = listOf(
            station(1, "Zulu"),
            station(2, "Alpha"),
            station(3, "Beta"),
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            sortHistoryStationsByDistance(
                stations,
                mapOf(1L to 10.0, 2L to 1.0, 3L to 3.0),
            ).map { it.stationId },
        )
        assertEquals(
            listOf(2L, 3L, 1L),
            sortHistoryStationsByDistance(stations, emptyMap()).map { it.stationId },
        )
    }

    @Test
    fun `period filter keeps only real points inside requested window`() {
        val now = LocalDateTime.of(2026, 9, 4, 12, 0)
        val points = listOf(
            point(now.minusMonths(4), 1900),
            point(now.minusMonths(2), 1950),
            point(now.minusDays(2), 2000),
        )
        assertEquals(
            listOf(2000L),
            filterHistoryPointsByPeriod(points, HistoryPeriod.ONE_MONTH, now)
                .map { it.priceMilliEuroPerUnit },
        )
        assertEquals(
            listOf(1950L, 2000L),
            filterHistoryPointsByPeriod(points, HistoryPeriod.THREE_MONTHS, now)
                .map { it.priceMilliEuroPerUnit },
        )
        assertEquals(3, filterHistoryPointsByPeriod(points, HistoryPeriod.ALL, now).size)
    }

    @Test
    fun `table merge aligns dates without inventing missing values`() {
        val t1 = LocalDateTime.of(2026, 8, 1, 8, 0)
        val t2 = LocalDateTime.of(2026, 8, 2, 8, 0)
        val t3 = LocalDateTime.of(2026, 8, 3, 8, 0)
        val rows = mergeHistorySeriesRows(
            seriesA = listOf(point(t1, 2000), point(t3, 2020)),
            seriesB = listOf(point(t2, 1800), point(t3, 1810)),
        )

        assertEquals(listOf(t1, t2, t3), rows.map { it.communicatedAt })
        assertEquals(2000L, rows[0].seriesAPriceMilliEuroPerUnit)
        assertNull(rows[0].seriesBPriceMilliEuroPerUnit)
        assertNull(rows[1].seriesAPriceMilliEuroPerUnit)
        assertEquals(1800L, rows[1].seriesBPriceMilliEuroPerUnit)
        assertEquals(2020L, rows[2].seriesAPriceMilliEuroPerUnit)
        assertEquals(1810L, rows[2].seriesBPriceMilliEuroPerUnit)
    }

    @Test
    fun `two series share price and time axes`() {
        val t1 = LocalDateTime.of(2026, 8, 1, 8, 0)
        val t2 = LocalDateTime.of(2026, 8, 2, 8, 0)
        val normalized = normalizeHistorySeries(
            seriesA = listOf(point(t1, 2000), point(t2, 2100)),
            seriesB = listOf(point(t1, 1800), point(t2, 1900)),
        )

        assertEquals(1800L, normalized.minPrice)
        assertEquals(2100L, normalized.maxPrice)
        assertEquals(0f, normalized.seriesA.first().xFraction)
        assertEquals(1f, normalized.seriesB.last().xFraction)
    }

    private fun station(id: Long, name: String = "Station $id") = PriceHistoryStation(
        stationId = id,
        name = name,
        address = "Via $id",
        municipality = "Comune",
        province = "MO",
    )

    private fun point(time: LocalDateTime, price: Long) = PriceHistoryPoint(
        communicatedAt = time,
        priceMilliEuroPerUnit = price,
        importedAtEpochMillis = 1L,
    )
}