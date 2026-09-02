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
    fun repairsKnownLiveUnescapedStationTextFragment() {
        val text = """
            Estrazione del 2026-09-01
            idImpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
            40820|STOIL SIMPLE|Pompe Bianche|Stradale|STOIL SIMPLE | gestori.prezzibenzina.it|STR. PROV.LE 82 SPINETTA SALE  15122|ALESSANDRIA|AL|44.91704718250436|8.70067298412323
        """.trimIndent()

        val station = parser.parseStations(text).rows.single()

        assertEquals(40820L, station.id)
        assertEquals("STOIL SIMPLE | gestori.prezzibenzina.it", station.name)
        assertEquals("STR. PROV.LE 82 SPINETTA SALE  15122", station.address)
    }

    @Test
    fun repairsTwoKnownLiveFragmentsInSameStationRecord() {
        val text = """
            Estrazione del 2026-09-01
            idImpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
            54386|PRADELLI - MONTEOMBRARO | gestori.prezzibenzina.it|Pompe Bianche|Stradale|PRADELLI - MONTEOMBRARO | gestori.prezzibenzina.it|Via dei Martiri 255 41059|ZOCCA|MO|44.37912317059676|11.004677838023897
        """.trimIndent()

        val station = parser.parseStations(text).rows.single()

        assertEquals("PRADELLI - MONTEOMBRARO | gestori.prezzibenzina.it", station.manager)
        assertEquals("PRADELLI - MONTEOMBRARO | gestori.prezzibenzina.it", station.name)
        assertEquals("Via dei Martiri 255 41059", station.address)
    }

    @Test
    fun stillRejectsUnknownExtraStationField() {
        val text = """
            Estrazione del 2026-09-01
            idImpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
            1|Gestore|Eni|Stradale|Nome|campo-extra-sconosciuto|Via Uno|Roma|RM|41.9|12.5
        """.trimIndent()

        val error = assertThrows(MimitCsvFormatException::class.java) {
            parser.parseStations(text)
        }

        assertTrue(error.message.orEmpty().contains("11 fields; expected 10"))
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
