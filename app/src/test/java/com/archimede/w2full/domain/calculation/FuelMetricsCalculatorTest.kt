package com.archimede.w2full.domain.calculation

import com.archimede.w2full.domain.model.Rifornimento
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelMetricsCalculatorTest {
    @Test
    fun `full to full window includes partial refuels`() {
        val metrics = FuelMetricsCalculator.calculate(
            records = listOf(
                refuel(id = 1, km = 10_000, litersMl = 50_000, cents = 9_000, full = true),
                refuel(id = 2, km = 10_300, litersMl = 20_000, cents = 4_000, full = false),
                refuel(id = 3, km = 10_600, litersMl = 25_000, cents = 5_000, full = true),
            ),
            tankCapacityMilliliters = 50_000,
        )

        assertEquals(7.5, metrics.averageConsumptionLPer100Km!!, 0.0001)
        assertEquals(0.15, metrics.costPerKmEuro!!, 0.0001)
        assertEquals(50.0, metrics.estimatedRemainingLiters!!, 0.0001)
        assertEquals(666.6667, metrics.estimatedRangeKm!!, 0.001)
    }

    @Test
    fun `range uses partial fuel added after latest full`() {
        val metrics = FuelMetricsCalculator.calculate(
            records = listOf(
                refuel(id = 1, km = 10_000, litersMl = 50_000, cents = 9_000, full = true),
                refuel(id = 2, km = 10_300, litersMl = 20_000, cents = 4_000, full = false),
                refuel(id = 3, km = 10_600, litersMl = 25_000, cents = 5_000, full = true),
                refuel(id = 4, km = 10_900, litersMl = 10_000, cents = 2_000, full = false),
            ),
            tankCapacityMilliliters = 50_000,
        )

        assertEquals(7.5, metrics.averageConsumptionLPer100Km!!, 0.0001)
        assertEquals(37.5, metrics.estimatedRemainingLiters!!, 0.0001)
        assertEquals(500.0, metrics.estimatedRangeKm!!, 0.0001)
    }

    @Test
    fun `metrics remain unavailable without two full anchors`() {
        val metrics = FuelMetricsCalculator.calculate(
            records = listOf(
                refuel(id = 1, km = 1_000, litersMl = 20_000, cents = 4_000, full = false),
                refuel(id = 2, km = 1_200, litersMl = 30_000, cents = 6_000, full = true),
            ),
            tankCapacityMilliliters = 50_000,
        )

        assertNull(metrics.averageConsumptionLPer100Km)
        assertNull(metrics.costPerKmEuro)
        assertNull(metrics.estimatedRemainingLiters)
        assertNull(metrics.estimatedRangeKm)
    }

    @Test
    fun `remaining fuel is clamped to tank capacity`() {
        val metrics = FuelMetricsCalculator.calculate(
            records = listOf(
                refuel(id = 1, km = 1_000, litersMl = 40_000, cents = 7_000, full = true),
                refuel(id = 2, km = 1_500, litersMl = 35_000, cents = 6_000, full = true),
                refuel(id = 3, km = 1_501, litersMl = 20_000, cents = 3_500, full = false),
            ),
            tankCapacityMilliliters = 40_000,
        )

        assertEquals(40.0, metrics.estimatedRemainingLiters!!, 0.0001)
    }

    @Test
    fun `price per liter is derived and not persisted`() {
        val price = FuelMetricsCalculator.pricePerLiterEuro(
            refuel(id = 1, km = 1_000, litersMl = 25_000, cents = 5_000, full = true),
        )

        assertEquals(2.0, price!!, 0.0001)
    }

    private fun refuel(
        id: Long,
        km: Long,
        litersMl: Long,
        cents: Long,
        full: Boolean,
    ) = Rifornimento(
        id = id,
        timestampEpochMillis = id * 1_000,
        odometerKm = km,
        litersMilliliters = litersMl,
        totalCostCents = cents,
        fuelType = "Benzina",
        isFullTank = full,
    )
}
