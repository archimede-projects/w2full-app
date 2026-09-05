package com.archimede.w2full.ui.history

import com.archimede.w2full.data.repository.PriceHistoryPoint
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun flatVisiblePricesStillProduceNonZeroYAxisWithFiveTicks() {
        val day = LocalDate.of(2026, 9, 3).toEpochDay()
        val viewport = HistoryChartViewport(day.toDouble(), day + 1.0)
        val axis = historyPriceAxisScale(
            seriesA = listOf(point(day, 2_069L)),
            seriesB = listOf(point(day, 2_069L)),
            viewport = viewport,
        ) ?: error("axis expected")

        assertTrue(axis.minPriceMilliEuro < 2_069L)
        assertTrue(axis.maxPriceMilliEuro > 2_069L)
        assertEquals(5, axis.ticksMilliEuro.size)
    }

    @Test
    fun yAxisUsesOnlyPointsInsideViewport() {
        val start = LocalDate.of(2026, 9, 1).toEpochDay()
        val series = listOf(
            point(start, 1_500L),
            point(start + 1, 1_600L),
            point(start + 9, 5_000L),
        )
        val axis = historyPriceAxisScale(
            seriesA = series,
            seriesB = emptyList(),
            viewport = HistoryChartViewport(start.toDouble(), start + 1.0),
        ) ?: error("axis expected")

        assertTrue(axis.maxPriceMilliEuro < 5_000L)
        assertTrue(axis.minPriceMilliEuro <= 1_500L)
        assertTrue(axis.maxPriceMilliEuro >= 1_600L)
    }

    @Test
    fun nearestDaySelectionReturnsBothSeriesAndMissingAsNull() {
        val start = LocalDate.of(2026, 9, 1).toEpochDay()
        val viewport = HistoryChartViewport(start.toDouble(), start + 4.0)
        val seriesA = listOf(point(start, 2_000L), point(start + 4, 2_100L))
        val seriesB = listOf(point(start, 1_800L))

        val selected = historyChartSelection(seriesA, seriesB, viewport, 0.95f)
            ?: error("selection expected")

        assertEquals(start + 4, selected.epochDay)
        assertEquals(2_100L, selected.seriesAPriceMilliEuro)
        assertNull(selected.seriesBPriceMilliEuro)
    }

    @Test
    fun pinchZoomIsClampedInsideAvailableBounds() {
        val bounds = HistoryChartBounds(minEpochDay = 100, maxEpochDay = 110)
        val full = defaultHistoryChartViewport(bounds)
        val zoomed = zoomHistoryViewport(full, bounds, zoomFactor = 4f, focusFraction = 0.5f)

        assertTrue(zoomed.spanDays < full.spanDays)
        assertTrue(zoomed.startEpochDay >= 100.0)
        assertTrue(zoomed.endEpochDay <= 110.0)

        val extreme = zoomHistoryViewport(zoomed, bounds, zoomFactor = 100f, focusFraction = 1f)
        assertTrue(extreme.spanDays >= 1.0)
        assertTrue(extreme.endEpochDay <= 110.0)
    }

    @Test
    fun panCannotMoveViewportOutsideBoundsAndResetRestoresFullWindow() {
        val bounds = HistoryChartBounds(minEpochDay = 100, maxEpochDay = 110)
        val zoomed = HistoryChartViewport(103.0, 106.0)
        val pannedRight = panHistoryViewport(zoomed, bounds, dragFraction = -10f)
        val pannedLeft = panHistoryViewport(zoomed, bounds, dragFraction = 10f)

        assertEquals(110.0, pannedRight.endEpochDay, 0.0001)
        assertEquals(100.0, pannedLeft.startEpochDay, 0.0001)
        assertEquals(HistoryChartViewport(100.0, 110.0), defaultHistoryChartViewport(bounds))
    }

    @Test
    fun dateTicksStayInsideViewport() {
        val viewport = HistoryChartViewport(100.0, 110.0)
        val ticks = historyDateTicks(viewport, tickCount = 3)

        assertEquals(listOf(100L, 105L, 110L), ticks)
    }

    private fun point(epochDay: Long, price: Long): PriceHistoryPoint {
        val date = LocalDate.ofEpochDay(epochDay)
        return PriceHistoryPoint(
            communicatedAt = LocalDateTime.of(date, java.time.LocalTime.NOON),
            priceMilliEuroPerUnit = price,
            importedAtEpochMillis = 1L,
            observedOn = date,
        )
    }
}
