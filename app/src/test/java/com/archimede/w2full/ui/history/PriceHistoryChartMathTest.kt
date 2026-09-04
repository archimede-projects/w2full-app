package com.archimede.w2full.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceHistoryChartMathTest {
    @Test
    fun emptySeriesProducesNoPoints() {
        assertTrue(normalizePriceHistory(emptyList()).isEmpty())
    }

    @Test
    fun singlePointIsCentered() {
        assertEquals(
            listOf(NormalizedPricePoint(0.5f, 0.5f)),
            normalizePriceHistory(listOf(1_789L)),
        )
    }

    @Test
    fun multiplePointsPreserveOrderAndNormalizeRange() {
        assertEquals(
            listOf(
                NormalizedPricePoint(0f, 1f),
                NormalizedPricePoint(0.5f, 0.5f),
                NormalizedPricePoint(1f, 0f),
            ),
            normalizePriceHistory(listOf(1_700L, 1_750L, 1_800L)),
        )
    }

    @Test
    fun flatSeriesUsesMiddleLine() {
        assertEquals(
            listOf(
                NormalizedPricePoint(0f, 0.5f),
                NormalizedPricePoint(1f, 0.5f),
            ),
            normalizePriceHistory(listOf(1_700L, 1_700L)),
        )
    }
}
