package com.archimede.w2full.ui.history

import com.archimede.w2full.data.repository.PriceHistoryPoint
import java.time.ZoneOffset

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
    val minTime = all.minOf { it.communicatedAt.toEpochSecond(ZoneOffset.UTC) }
    val maxTime = all.maxOf { it.communicatedAt.toEpochSecond(ZoneOffset.UTC) }
    val priceRange = maxPrice - minPrice
    val timeRange = maxTime - minTime

    fun normalize(points: List<PriceHistoryPoint>): List<NormalizedPricePoint> = points.map { point ->
        val epoch = point.communicatedAt.toEpochSecond(ZoneOffset.UTC)
        val x = if (timeRange == 0L) 0.5f else (epoch - minTime).toFloat() / timeRange.toFloat()
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