package com.archimede.w2full.data.mimit

import java.time.LocalDateTime
import java.util.Locale

enum class MimitPriceUnit(val label: String) {
    LITER("€/L"),
    KILOGRAM("€/kg"),
}

data class MimitSelectedModePrice(
    val priceMilliEuroPerUnit: Long,
    val communicatedAt: LocalDateTime,
)

data class MimitStationFuelPrice(
    val fuelType: String,
    val unit: MimitPriceUnit,
    val self: MimitSelectedModePrice?,
    val served: MimitSelectedModePrice?,
)

data class MimitStationPriceSelection(
    val fuelType: String,
    val pricesByStationId: Map<Long, MimitStationFuelPrice>,
)

object MimitStationPriceSelector {
    const val FALLBACK_FUEL_TYPE = "Benzina"

    private val whitespace = Regex("\\s+")

    fun select(
        prices: List<MimitPrice>,
        defaultFuelType: String?,
    ): MimitStationPriceSelection {
        val fuelType = displayFuelType(defaultFuelType)
        val normalizedFuelType = normalize(fuelType)
        val unit = if (normalizedFuelType == "metano") {
            MimitPriceUnit.KILOGRAM
        } else {
            MimitPriceUnit.LITER
        }

        val matchingPrices = prices.filter { normalize(it.fuelDescription) == normalizedFuelType }
        val pricesByStationId = matchingPrices
            .groupBy { it.stationId }
            .mapValues { (_, stationPrices) ->
                MimitStationFuelPrice(
                    fuelType = fuelType,
                    unit = unit,
                    self = stationPrices
                        .filter { it.isSelf }
                        .maxByOrNull { it.communicatedAt }
                        ?.toSelectedModePrice(),
                    served = stationPrices
                        .filterNot { it.isSelf }
                        .maxByOrNull { it.communicatedAt }
                        ?.toSelectedModePrice(),
                )
            }
            .filterValues { it.self != null || it.served != null }

        return MimitStationPriceSelection(
            fuelType = fuelType,
            pricesByStationId = pricesByStationId,
        )
    }

    internal fun normalize(value: String): String = value
        .trim()
        .replace(whitespace, " ")
        .lowercase(Locale.ROOT)

    private fun displayFuelType(value: String?): String {
        val compact = value
            ?.trim()
            ?.replace(whitespace, " ")
            .orEmpty()
        return compact.ifBlank { FALLBACK_FUEL_TYPE }
    }

    private fun MimitPrice.toSelectedModePrice() = MimitSelectedModePrice(
        priceMilliEuroPerUnit = priceMilliEuroPerUnit,
        communicatedAt = communicatedAt,
    )
}
