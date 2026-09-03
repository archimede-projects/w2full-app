package com.archimede.w2full.data.mimit

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MimitStationFilterTest {
    @Test
    fun recognizesOnlyNormalizedExactEniAliases() {
        assertTrue(MimitStationFilter.isEniBrand("Eni"))
        assertTrue(MimitStationFilter.isEniBrand(" ENI "))
        assertTrue(MimitStationFilter.isEniBrand("eni"))
        assertTrue(MimitStationFilter.isEniBrand("\tEnI\n"))
        assertTrue(MimitStationFilter.isEniBrand("Agip Eni"))
        assertTrue(MimitStationFilter.isEniBrand("  AGIP   ENI  "))

        assertFalse(MimitStationFilter.isEniBrand("Q8"))
        assertFalse(MimitStationFilter.isEniBrand("Pompe Bianche"))
        assertFalse(MimitStationFilter.isEniBrand(""))
        assertFalse(MimitStationFilter.isEniBrand("Eni Plus"))
        assertFalse(MimitStationFilter.isEniBrand("SuperEni"))
        assertFalse(MimitStationFilter.isEniBrand("Agip Eni Plus"))
    }

    @Test
    fun filtersStationsAndPreservesOriginalOrder() {
        val stations = listOf(
            station(id = 1, brand = "Q8"),
            station(id = 2, brand = "Eni"),
            station(id = 3, brand = "Agip Eni"),
            station(id = 4, brand = "Pompe Bianche"),
            station(id = 5, brand = "  AGIP   ENI  "),
        )

        val filtered = MimitStationFilter.eniStations(stations)

        assertEquals(listOf(2L, 3L, 5L), filtered.map { it.id })
    }

    @Test
    fun filtersDatasetWithoutChangingExtractionDate() {
        val extractionDate = LocalDate.of(2026, 9, 1)
        val dataset = MimitDataset(
            extractionDate = extractionDate,
            rows = listOf(
                station(id = 10, brand = "Agip Eni"),
                station(id = 11, brand = "Q8"),
                station(id = 12, brand = "ENI"),
            ),
        )

        val filtered = MimitStationFilter.eniStations(dataset)

        assertEquals(extractionDate, filtered.extractionDate)
        assertEquals(listOf(10L, 12L), filtered.rows.map { it.id })
    }

    private fun station(id: Long, brand: String): MimitStation = MimitStation(
        id = id,
        manager = "Gestore $id",
        brand = brand,
        stationType = "Stradale",
        name = "Impianto $id",
        address = "Via Test $id",
        municipality = "Roma",
        province = "RM",
        latitude = null,
        longitude = null,
    )
}
