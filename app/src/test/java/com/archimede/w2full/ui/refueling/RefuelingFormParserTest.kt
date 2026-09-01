package com.archimede.w2full.ui.refueling

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RefuelingFormParserTest {
    @Test
    fun `parser converts comma decimals to integer storage units`() {
        val draft = RefuelingFormParser.parseDraft(
            dateText = "2026-09-01",
            odometerText = "12345",
            litersText = "42,750",
            totalCostText = "79,99",
            fuelTypeText = " Benzina ",
            isFullTank = true,
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(12_345, draft.odometerKm)
        assertEquals(42_750, draft.litersMilliliters)
        assertEquals(7_999, draft.totalCostCents)
        assertEquals("Benzina", draft.fuelType)
        assertEquals(
            LocalDate.of(2026, 9, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            draft.timestampEpochMillis,
        )
    }

    @Test
    fun `tank capacity is stored as milliliters`() {
        assertEquals(52_500, RefuelingFormParser.parseTankCapacityMilliliters("52,5"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parser rejects invalid odometer`() {
        RefuelingFormParser.parseDraft(
            dateText = "2026-09-01",
            odometerText = "-1",
            litersText = "40",
            totalCostText = "70",
            fuelTypeText = "Benzina",
            isFullTank = true,
            zoneId = ZoneOffset.UTC,
        )
    }
}
