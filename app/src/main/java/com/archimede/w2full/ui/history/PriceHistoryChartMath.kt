package com.archimede.w2full.ui.history

import com.archimede.w2full.data.repository.PriceHistoryPoint
import kotlin.math.abs
import kotlin.math.roundToLong

data class NormalizedPricePoint(
    val xFraction: Float,
    val yFraction: Float,
)

data class NormalizedHistorySeries(
    val seriesA: List<NormalizedPricePoint>,
    val seriesB: List<NormalizedPricePoint>,
    val minPrice: Long?,
    val maxPrice: Long?,
)

data class HistoryChartBounds(
    val minEpochDay: Long,
    val maxEpochDay: Long,
) {
    val spanDays: Double
        get() = (maxEpochDay - minEpochDay).coerceAtLeast(1L).toDouble()
}

data class HistoryChartViewport(
    val startEpochDay: Double,
    val endEpochDay: Double,
) {
    init {
        require(startEpochDay.isFinite() && endEpochDay.isFinite() && endEpochDay > startEpochDay)
    }

    val spanDays: Double
        get() = endEpochDay - startEpochDay
}

data class HistoryPriceAxisScale(
    val minPriceMilliEuro: Long,
    val maxPriceMilliEuro: Long,
    val ticksMilliEuro: List<Long>,
)

data class HistoryChartSelection(
    val epochDay: Long,
    val seriesAPriceMilliEuro: Long?,
    val seriesBPriceMilliEuro: Long?,
)

data class HistoryChartRenderPoint(
    val epochDay: Long,
    val priceMilliEuro: Long,
    val xFraction: Float,
    val yFraction: Float,
)

fun normalizePriceHistory(prices: List<Long>): List<NormalizedPricePoint> {
    if (prices.isEmpty()) return emptyList()
    if (prices.size == 1) return listOf(NormalizedPricePoint(0.5f, 0.5f))

    val min = prices.minOrNull() ?: return emptyList()
    val max = prices.maxOrNull() ?: return emptyList()
    val range = max - min

    return prices.mapIndexed { index, value ->
        val x = index.toFloat() / prices.lastIndex.toFloat()
        val y = if (range == 0L) 0.5f else 1f - ((value - min).toFloat() / range.toFloat())
        NormalizedPricePoint(x, y)
    }
}

internal fun normalizeHistorySeries(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
): NormalizedHistorySeries {
    val all = seriesA + seriesB
    if (all.isEmpty()) return NormalizedHistorySeries(emptyList(), emptyList(), null, null)

    val minPrice = all.minOf { it.priceMilliEuroPerUnit }
    val maxPrice = all.maxOf { it.priceMilliEuroPerUnit }
    val minDay = all.minOf { it.observedOn.toEpochDay() }
    val maxDay = all.maxOf { it.observedOn.toEpochDay() }
    val priceRange = maxPrice - minPrice
    val dayRange = maxDay - minDay

    fun normalize(points: List<PriceHistoryPoint>): List<NormalizedPricePoint> = points.map { point ->
        val day = point.observedOn.toEpochDay()
        val x = if (dayRange == 0L) 0.5f else (day - minDay).toFloat() / dayRange.toFloat()
        val y = if (priceRange == 0L) {
            0.5f
        } else {
            1f - ((point.priceMilliEuroPerUnit - minPrice).toFloat() / priceRange.toFloat())
        }
        NormalizedPricePoint(x, y)
    }

    return NormalizedHistorySeries(
        seriesA = normalize(seriesA),
        seriesB = normalize(seriesB),
        minPrice = minPrice,
        maxPrice = maxPrice,
    )
}

internal fun historyChartBounds(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
): HistoryChartBounds? {
    val all = seriesA + seriesB
    if (all.isEmpty()) return null
    return HistoryChartBounds(
        minEpochDay = all.minOf { it.observedOn.toEpochDay() },
        maxEpochDay = all.maxOf { it.observedOn.toEpochDay() },
    )
}

internal fun defaultHistoryChartViewport(bounds: HistoryChartBounds): HistoryChartViewport =
    HistoryChartViewport(
        startEpochDay = bounds.minEpochDay.toDouble(),
        endEpochDay = if (bounds.maxEpochDay > bounds.minEpochDay) {
            bounds.maxEpochDay.toDouble()
        } else {
            bounds.minEpochDay + 1.0
        },
    )

internal fun isHistoryViewportZoomed(
    viewport: HistoryChartViewport,
    bounds: HistoryChartBounds,
): Boolean = viewport.spanDays < bounds.spanDays - 0.001

internal fun zoomHistoryViewport(
    viewport: HistoryChartViewport,
    bounds: HistoryChartBounds,
    zoomFactor: Float,
    focusFraction: Float,
): HistoryChartViewport {
    if (!zoomFactor.isFinite() || zoomFactor <= 0f || bounds.spanDays <= 1.0) {
        return defaultHistoryChartViewport(bounds)
    }
    val fullSpan = bounds.spanDays
    val minSpan = (fullSpan / MAX_HISTORY_ZOOM).coerceAtLeast(1.0)
    val newSpan = (viewport.spanDays / zoomFactor.toDouble()).coerceIn(minSpan, fullSpan)
    val focus = focusFraction.coerceIn(0f, 1f).toDouble()
    val focusDay = viewport.startEpochDay + viewport.spanDays * focus
    var start = focusDay - newSpan * focus
    var end = start + newSpan
    val minBound = bounds.minEpochDay.toDouble()
    val maxBound = bounds.maxEpochDay.toDouble()

    if (start < minBound) {
        start = minBound
        end = start + newSpan
    }
    if (end > maxBound) {
        end = maxBound
        start = end - newSpan
    }
    if (newSpan >= fullSpan - 0.001) return defaultHistoryChartViewport(bounds)
    return HistoryChartViewport(start, end)
}

internal fun panHistoryViewport(
    viewport: HistoryChartViewport,
    bounds: HistoryChartBounds,
    dragFraction: Float,
): HistoryChartViewport {
    if (!isHistoryViewportZoomed(viewport, bounds) || !dragFraction.isFinite()) return viewport
    val shift = -dragFraction.toDouble() * viewport.spanDays
    var start = viewport.startEpochDay + shift
    var end = viewport.endEpochDay + shift
    val minBound = bounds.minEpochDay.toDouble()
    val maxBound = bounds.maxEpochDay.toDouble()

    if (start < minBound) {
        start = minBound
        end = start + viewport.spanDays
    }
    if (end > maxBound) {
        end = maxBound
        start = end - viewport.spanDays
    }
    return HistoryChartViewport(start, end)
}

internal fun visibleHistoryPoints(
    points: List<PriceHistoryPoint>,
    viewport: HistoryChartViewport,
): List<PriceHistoryPoint> = points.filter { point ->
    val day = point.observedOn.toEpochDay().toDouble()
    day >= viewport.startEpochDay - 0.0001 && day <= viewport.endEpochDay + 0.0001
}

internal fun historyPriceAxisScale(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    viewport: HistoryChartViewport,
    tickCount: Int = 5,
): HistoryPriceAxisScale? {
    require(tickCount >= 2)
    val prices = (visibleHistoryPoints(seriesA, viewport) + visibleHistoryPoints(seriesB, viewport))
        .map { it.priceMilliEuroPerUnit }
    if (prices.isEmpty()) return null

    val rawMin = prices.minOrNull() ?: return null
    val rawMax = prices.maxOrNull() ?: return null
    val range = rawMax - rawMin
    val margin = if (range == 0L) {
        MIN_PRICE_MARGIN_MILLI_EURO
    } else {
        (range * 0.12).roundToLong().coerceAtLeast(MIN_PRICE_MARGIN_MILLI_EURO / 2)
    }
    val axisMin = (rawMin - margin).coerceAtLeast(0L)
    val axisMax = (rawMax + margin).coerceAtLeast(axisMin + tickCount - 1L)
    val ticks = List(tickCount) { index ->
        val fraction = index.toDouble() / (tickCount - 1).toDouble()
        (axisMin + (axisMax - axisMin) * fraction).roundToLong()
    }
    return HistoryPriceAxisScale(axisMin, axisMax, ticks)
}

internal fun historyDateTicks(
    viewport: HistoryChartViewport,
    tickCount: Int = 3,
): List<Long> {
    require(tickCount >= 2)
    return List(tickCount) { index ->
        val fraction = index.toDouble() / (tickCount - 1).toDouble()
        (viewport.startEpochDay + viewport.spanDays * fraction).roundToLong()
    }.distinct()
}

internal fun historyChartSelection(
    seriesA: List<PriceHistoryPoint>,
    seriesB: List<PriceHistoryPoint>,
    viewport: HistoryChartViewport,
    xFraction: Float,
): HistoryChartSelection? {
    val visibleA = visibleHistoryPoints(seriesA, viewport)
    val visibleB = visibleHistoryPoints(seriesB, viewport)
    val candidateDays = (visibleA.map { it.observedOn.toEpochDay() } + visibleB.map { it.observedOn.toEpochDay() })
        .distinct()
    if (candidateDays.isEmpty()) return null

    val target = viewport.startEpochDay + viewport.spanDays * xFraction.coerceIn(0f, 1f)
    val selectedDay = candidateDays.minByOrNull { abs(it.toDouble() - target) } ?: return null
    return HistoryChartSelection(
        epochDay = selectedDay,
        seriesAPriceMilliEuro = visibleA.lastOrNull { it.observedOn.toEpochDay() == selectedDay }?.priceMilliEuroPerUnit,
        seriesBPriceMilliEuro = visibleB.lastOrNull { it.observedOn.toEpochDay() == selectedDay }?.priceMilliEuroPerUnit,
    )
}

internal fun historyRenderPoints(
    points: List<PriceHistoryPoint>,
    viewport: HistoryChartViewport,
    axis: HistoryPriceAxisScale,
): List<HistoryChartRenderPoint> {
    val priceRange = (axis.maxPriceMilliEuro - axis.minPriceMilliEuro).coerceAtLeast(1L).toDouble()
    return visibleHistoryPoints(points, viewport).map { point ->
        val day = point.observedOn.toEpochDay()
        val x = ((day - viewport.startEpochDay) / viewport.spanDays).toFloat().coerceIn(0f, 1f)
        val y = (1.0 - (point.priceMilliEuroPerUnit - axis.minPriceMilliEuro) / priceRange)
            .toFloat()
            .coerceIn(0f, 1f)
        HistoryChartRenderPoint(
            epochDay = day,
            priceMilliEuro = point.priceMilliEuroPerUnit,
            xFraction = x,
            yFraction = y,
        )
    }
}

private const val MAX_HISTORY_ZOOM = 30.0
private const val MIN_PRICE_MARGIN_MILLI_EURO = 10L
