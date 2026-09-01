package com.archimede.w2full.data.mimit

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MimitCsvParserTest {
    private val parser = MimitCsvParser()

    @Test
    fun parsesStationFixtureWithQuotedPipeEscapedQuotesAndNullableCoordinates() {
        val dataset = parser.parseStations(resourceText("mimit/anagrafica_sample.csv"))

        assertEquals(LocalDate.of(2026, 9, 1), dataset.extractionDate)
        assertEquals(3, dataset.rows.size)

        val first = dataset.rows[0]
        assertEquals(12345L, first.id)
        assertEquals("Eni", first.brand)
        assertEquals("Stazione | Centro", first.name)
        assertEquals(41.9028, first.latitude!!, 0.000001)
        assertEquals(12.4964, first.longitude!!, 0.000001)

        val second = dataset.rows[1]
        assertEquals("ACME \"Fuel\" SRL", second.manager)
        assertNull(second.latitude)
        assertNull(second.longitude)
    }

    @Test
    fun parsesPriceFixtureWithoutFloatingPointPersistenceLoss() {
        val dataset = parser.parsePrices(resourceText("mimit/prezzi_sample.csv"))

        assertEquals(LocalDate.of(2026, 9, 1), dataset.extractionDate)
        assertEquals(3, dataset.rows.size)

        val petrol = dataset.rows[0]
        assertEquals(12345L, petrol.stationId)
        assertEquals("Benzina", petrol.fuelDescription)
        assertEquals(1789L, petrol.priceMilliEuroPerUnit)
        assertTrue(petrol.isSelf)
        assertEquals(LocalDateTime.of(2026, 9, 1, 7, 45, 12), petrol.communicatedAt)

        val diesel = dataset.rows[1]
        assertEquals(1699L, diesel.priceMilliEuroPerUnit)
        assertFalse(diesel.isSelf)
    }

    @Test
    fun acceptsIdImpiantoHeaderCaseVariation() {
        val text = """
            Estrazione del 2026-09-01
            IDIMPIANTO|DESCcarburante|PREZZO|ISSELF|DTCOMU
            1|Benzina|1.700|1|01/09/2026 08:00:00
        """.trimIndent()

        val dataset = parser.parsePrices(text)

        assertEquals(1L, dataset.rows.single().stationId)
    }

    @Test
    fun rejectsMissingRequiredHeader() {
        val text = """
            Estrazione del 2026-09-01
            idimpianto|descCarburante|prezzo|dtComu
            1|Benzina|1.700|01/09/2026 08:00:00
        """.trimIndent()

        assertThrows(MimitCsvFormatException::class.java) {
            parser.parsePrices(text)
        }
    }

    @Test
    fun rejectsUnexpectedSelfServiceValue() {
        val text = """
            Estrazione del 2026-09-01
            idimpianto|descCarburante|prezzo|isSelf|dtComu
            1|Benzina|1.700|yes|01/09/2026 08:00:00
        """.trimIndent()

        assertThrows(MimitCsvFormatException::class.java) {
            parser.parsePrices(text)
        }
    }

    private fun resourceText(path: String): String = requireNotNull(
        javaClass.classLoader?.getResource(path),
    ) { "Missing test resource $path" }.readText()
}
