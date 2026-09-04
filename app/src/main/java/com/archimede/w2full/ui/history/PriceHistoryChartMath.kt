package com.archimede.w2full.ui.history

data class NormalizedPricePoint(
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
        val x = index.toFloat() / (prices.lastIndex.toFloat())
        val y = if (range == 0L) {
            0.5f
        } else {
            1f - ((value - min).toFloat() / range.toFloat())
        }
        NormalizedPricePoint(x, y)
    }
}
