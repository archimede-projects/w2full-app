package com.archimede.w2full.domain.calculation

import com.archimede.w2full.domain.model.Rifornimento

data class FuelMetrics(
    val averageConsumptionLPer100Km: Double?,
    val costPerKmEuro: Double?,
    val estimatedRemainingLiters: Double?,
    val estimatedRangeKm: Double?,
)

object FuelMetricsCalculator {
    private val recordOrder = compareBy<Rifornimento> { it.odometerKm }
        .thenBy { it.timestampEpochMillis }
        .thenBy { it.id }

    fun calculate(
        records: List<Rifornimento>,
        tankCapacityMilliliters: Long?,
    ): FuelMetrics {
        val ordered = records.sortedWith(recordOrder)
        val fullIndices = ordered.indices.filter { ordered[it].isFullTank }

        var averageConsumption: Double? = null
        var costPerKm: Double? = null

        if (fullIndices.size >= 2) {
            val startIndex = fullIndices.first()
            val endIndex = fullIndices.last()
            val distanceKm = ordered[endIndex].odometerKm - ordered[startIndex].odometerKm
            val closedWindow = ordered.subList(startIndex + 1, endIndex + 1)
            val consumedMilliliters = closedWindow.sumOf { it.litersMilliliters }
            val consumedCostCents = closedWindow.sumOf { it.totalCostCents }

            if (distanceKm > 0 && consumedMilliliters > 0) {
                val consumedLiters = consumedMilliliters / 1000.0
                averageConsumption = consumedLiters / distanceKm * 100.0
                costPerKm = (consumedCostCents / 100.0) / distanceKm
            }
        }

        val capacityLiters = tankCapacityMilliliters
            ?.takeIf { it > 0 }
            ?.div(1000.0)
        val latestFullIndex = ordered.indexOfLast { it.isFullTank }

        if (
            averageConsumption == null ||
            averageConsumption <= 0.0 ||
            capacityLiters == null ||
            latestFullIndex < 0
        ) {
            return FuelMetrics(
                averageConsumptionLPer100Km = averageConsumption,
                costPerKmEuro = costPerKm,
                estimatedRemainingLiters = null,
                estimatedRangeKm = null,
            )
        }

        val fullAnchor = ordered[latestFullIndex]
        val recordsAfterFull = ordered.drop(latestFullIndex + 1)
        val latestKnownOdometer = maxOf(
            fullAnchor.odometerKm,
            recordsAfterFull.maxOfOrNull { it.odometerKm } ?: fullAnchor.odometerKm,
        )
        val distanceSinceFullKm = latestKnownOdometer - fullAnchor.odometerKm
        val estimatedConsumedLiters = distanceSinceFullKm * averageConsumption / 100.0
        val partialAddedLiters = recordsAfterFull
            .filterNot { it.isFullTank }
            .sumOf { it.litersMilliliters } / 1000.0
        val remainingLiters = (
            capacityLiters - estimatedConsumedLiters + partialAddedLiters
        ).coerceIn(0.0, capacityLiters)
        val rangeKm = remainingLiters / averageConsumption * 100.0

        return FuelMetrics(
            averageConsumptionLPer100Km = averageConsumption,
            costPerKmEuro = costPerKm,
            estimatedRemainingLiters = remainingLiters,
            estimatedRangeKm = rangeKm,
        )
    }

    fun pricePerLiterEuro(record: Rifornimento): Double? {
        if (record.litersMilliliters <= 0 || record.totalCostCents < 0) return null
        return (record.totalCostCents / 100.0) / (record.litersMilliliters / 1000.0)
    }
}
